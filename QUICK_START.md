# 📖 E-Commerce Saga Pattern - 快速启动指南

## 🎬 五分钟快速启动

### 前提条件检查

```bash
# 检查 Docker
docker --version
docker compose --version

# 应该输出类似:
# Docker version 24.0.0+
# Docker Compose version v2.20.0+
```

### 一键启动 (推荐)

```bash
# 进入项目目录
cd /Users/logcabin/Workspace/uwindsor/SRS_Ecommerce_Saga

# 启动所有服务
docker compose up --build

# 输出示例:
# ✓ RabbitMQ 启动完成 (http://localhost:15672)
# ✓ Order DB 启动完成
# ✓ Inventory DB 启动完成
# ✓ Order Service 启动完成 (http://localhost:8080)
# ✓ Inventory Service 启动完成 (http://localhost:8081)
# ✓ Payment Service 启动完成 (http://localhost:8083)
```

### 访问系统

| 组件           | URL                                | 用途                        |
| -------------- | ---------------------------------- | --------------------------- |
| 📊 Dashboard   | http://localhost:8080/dashboard    | 实时仪表板（2秒刷新）       |
| 📮 RabbitMQ    | http://localhost:15672             | 消息队列管理（guest/guest） |
| 💳 Payment API | http://localhost:8083/api/payments | 支付、钱包与支付记录接口    |
| 📝 API 文档    | Postman_Collection.json            | 导入Postman测试             |

---

## 🧪 测试Saga流程

### 场景1: 成功路径 ✅

```bash
# 1. 创建订单 (库存充足)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "items": [{"productId": 1, "quantity": 2}]
  }'

# 响应示例:
# {
#   "orderId": "ORD-A1B2C3D4",
#   "status": "PENDING"
# }

# 2. 查看仪表板并完成支付
# 打开: http://localhost:8080/dashboard
# 观察: 订单从 PENDING → CONFIRMED (2-5秒内)
# 点击订单行中的 "Pay Now"
# 观察: 订单变为 PAID，Payment Results 出现 SUCCESS 记录

# 3. 查询最终订单状态
curl http://localhost:8080/api/orders/ORD-A1B2C3D4
# 结果: { "status": "PAID" }
```

### 场景2: 支付失败补偿路径 ⚠️

```bash
# 1. 创建订单（正常库存）
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1002,
    "items": [{"productId": 1, "quantity": 1}]
  }'

# 响应: { "orderId": "ORD-X2Y3Z4W5", "status": "PENDING" }

# 2. 在Dashboard把 userId=1002 的钱包余额设置为很小（例如 1）
# 3. 点击该订单 "Pay Now"
# 4. 查看仪表板
# 打开: http://localhost:8080/dashboard
# 观察: 订单 CONFIRMED → CANCELLED
# 观察: Payment Results 出现 FAILED 记录
# 观察: 库存 reserved 被回滚

# 5. 查询最终订单状态
curl http://localhost:8080/api/orders/ORD-X2Y3Z4W5
# 结果: { "status": "CANCELLED" }
```

### 场景3: 一键清理测试数据 🧹

```bash
# 清理订单、支付、钱包、库存日志（保留产品数据）
curl -X DELETE http://localhost:8081/api/inventory/test-data
curl -X DELETE http://localhost:8083/api/payments/test-data
curl -X DELETE http://localhost:8080/api/orders/test-data
```

### 场景4: 并发订单 🔄

```bash
# 快速创建多个订单
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{
      "userId": 2000,
      "items": [
        {"productId": 1, "quantity": 1},
        {"productId": 2, "quantity": 1},
        {"productId": 3, "quantity": 1}
      ]
    }' &
done
wait

# 查看仪表板观察并发处理
# 应该看到所有5个订单在2-5秒内完成
```

---

## 🔍 监控与调试

### 实时查看日志

```bash
# Order Service日志
docker logs saga-order-service -f

# Inventory Service日志
docker logs saga-inventory-service -f

# Payment Service日志
docker logs saga-payment-service -f

# RabbitMQ日志
docker logs saga-rabbitmq -f
```

### 检查消息队列

访问 http://localhost:15672，使用凭证 `guest/guest`

1. **Queues** 标签：
   - ✓ `order.created.queue` - Order Service发布，Inventory Service消费
   - ✓ `inventory.reserved.queue` - Inventory Service发布，Order Service消费
   - ✓ `inventory.failed.queue` - 仅在失败时使用

   - ✓ `payment.reserved.queue` - Payment Service发布，Order Service消费
   - ✓ `payment.failed.queue` - Payment失败补偿路径
   - ✓ `order.cancelled.queue` - Order Service发布，Inventory Service消费

2. **Exchanges** 标签：
   - ✓ `saga.events` - Topic Exchange 用于所有事件

### 数据库查询

```bash
# 查看所有订单
mysql -h localhost -P 3306 -u root -proot order_db -e \
  "SELECT order_id, user_id, status, created_at FROM orders ORDER BY created_at DESC LIMIT 10;"

# 查看所有Saga日志
mysql -h localhost -P 3306 -u root -proot order_db -e \
  "SELECT saga_id, current_step, status, last_updated FROM saga_log;"

# 检查库存状态
mysql -h localhost -P 3307 -u root -proot inventory_db -e \
  "SELECT product_name, stock, reserved, (stock - reserved) as available FROM inventory;"

# 查看库存操作日志
mysql -h localhost -P 3307 -u root -proot inventory_db -e \
  "SELECT order_id, product_id, quantity, action, timestamp FROM inventory_log ORDER BY timestamp DESC LIMIT 10;"
```

---

## 🛠️ 常见问题

### Q: 端口被占用如何处理?

```bash
# A: 查找占用进程
lsof -i :8080  # Order Service
lsof -i :8081  # Inventory Service
lsof -i :8083  # Payment Service
lsof -i :5672  # RabbitMQ
lsof -i :3306  # Order DB
lsof -i :3307  # Inventory DB
lsof -i :3308  # Payment DB

# 杀死进程
kill -9 <PID>

# 或者重新启动Docker
docker compose down -v
docker compose up --build
```

### Q: 服务无法连接到数据库?

```bash
# A: 检查容器是否运行
docker ps

# 查看容器日志
docker logs saga-order-db
docker logs saga-inventory-db

# 验证数据库连接
mysql -h localhost -P 3306 -u root -proot -e "SELECT 1"
mysql -h localhost -P 3307 -u root -proot -e "SELECT 1"
mysql -h localhost -P 3308 -u root -proot -e "SELECT 1"
```

### Q: RabbitMQ消息堆积如何清理?

```bash
# A: 访问管理界面 http://localhost:15672
# 1. 进入 Queues 标签
# 2. 选择队列 (order.created.queue 等)
# 3. 点击 "Purge Messages"
# 或使用命令:
docker exec saga-rabbitmq rabbitmqctl purge_queue order.created.queue
```

### Q: 如何完全重置系统?

```bash
# A: 删除所有容器和卷
docker compose down -v

# 删除镜像 (可选)
docker compose down --rmi all

# 重新构建并启动
docker compose up --build
```

---

## 📊 性能指标

| 指标             | 值        | 说明               |
| ---------------- | --------- | ------------------ |
| 订单创建时间     | <100ms    | 本地ACID事务       |
| 库存保留时间     | <500ms    | 单个服务事务       |
| 端到端Saga完成   | 2-5秒     | 包括网络和消息延迟 |
| 补偿路径完成     | 2-5秒     | 与成功路径相同     |
| 并发订单能力     | 100+      | 同时处理能力       |
| RabbitMQ消息吞吐 | 1000+/sec | 消息队列吞吐量     |

---

## 🏗️ Saga执行流程详解

### 订单确认流程

```
时间轴：
0ms  →  用户创建订单
     ├─ Order Service: 创建订单 (PENDING)
     ├─ Order Service: 记录Saga日志 (STARTED)
     └─ Order Service: 发布 OrderCreatedEvent

100ms →  RabbitMQ消息传递
     └─ Message in queue: order.created.queue

200ms →  Inventory Service处理
     ├─ 接收 OrderCreatedEvent
     ├─ 检查库存: iPhone 16 有1000个
     ├─ 预留2个 (stock: 1000, reserved: 2)
     └─ 发布 InventoryReservedEvent

300ms →  RabbitMQ消息返回
     └─ Message in queue: inventory.reserved.queue

400ms →  Order Service更新中间状态
     ├─ 接收 InventoryReservedEvent
  ├─ 订单状态: CONFIRMED
  └─ Saga状态: AWAITING_PAYMENT

600ms →  Payment Service处理
  ├─ 前端调用 /api/payments/{orderId}?status=SUCCESS
  ├─ 保存支付记录
  └─ 发布 PaymentReservedEvent

800ms →  Order Service最终完成
  ├─ 接收 PaymentReservedEvent
  ├─ 订单状态: PAID
  └─ Saga状态: COMPLETED

总耗时: 800ms (实际2-5秒含网络延迟)
```

### 订单取消流程 (补偿)

```
时间轴：
0ms  →  用户创建订单 (库存不足)
     ├─ Order Service: 创建订单 (PENDING)
     ├─ Order Service: 记录Saga日志 (STARTED)
     └─ Order Service: 发布 OrderCreatedEvent

200ms →  Inventory Service检测故障
     ├─ 接收 OrderCreatedEvent
     ├─ 检查库存: iPhone 16 仅1000个
     ├─ 要求预留: 10000个 ✗ 失败
     └─ 发布 InventoryReservationFailedEvent

400ms →  Order Service触发补偿
  ├─ 接收 InventoryReservationFailedEvent
  ├─ 订单状态: CANCELLED (补偿)
  └─ Saga状态: COMPENSATED

支付失败补偿（库存已预留场景）：
600ms → Payment Service发布 PaymentReservationFailedEvent
700ms → Order Service发布 OrderCancelledEvent
800ms → Inventory Service释放 reserved 库存
900ms → Order状态保持 CANCELLED

总耗时: 400ms (实际2-5秒含网络延迟)
注意: 无需额外补偿，因为库存从未预留过
```

---

## 📈 下一步改进

1. **分布式追踪**: 添加 Sleuth + Zipkin
2. **断路器**: Resilience4j 处理服务故障
3. **重试策略**: 指数退避重试机制
4. **死信队列**: 处理失败消息
5. **监控告警**: Prometheus + Grafana
6. **持久化**: 事件溯源(Event Sourcing)
7. **缓存**: Redis缓存库存查询

---

## ✅ 验收清单

- [ ] 所有Docker容器成功启动
- [ ] 仪表板能够访问 (http://localhost:8080/dashboard)
- [ ] 成功订单流程正常 (PENDING → CONFIRMED → PAID)
- [ ] 失败订单流程正常 (PENDING → CANCELLED)
- [ ] 支付失败后库存补偿正常 (OrderCancelledEvent)
- [ ] RabbitMQ消息队列有消息流动
- [ ] 数据库中有订单和Saga日志记录
- [ ] 并发订单处理正常
- [ ] 日志中无错误信息

---

**祝您使用愉快！如有问题，请查阅完整的 PROJECT_README.md 文件。**
