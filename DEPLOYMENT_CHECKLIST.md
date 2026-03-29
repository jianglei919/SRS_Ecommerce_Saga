# ✅ 部署前检查清单

## 系统需求验证

- [ ] **Docker版本** ≥ 24.0.0

  ```bash
  docker --version
  ```

- [ ] **Docker Compose版本** ≥ v2.20.0

  ```bash
  docker compose --version
  ```

- [ ] **可用端口检查**

  ```bash
  # 这些端口应该是空闲的
  lsof -i :8080   # Order Service
  lsof -i :8081   # Inventory Service
  lsof -i :8083   # Payment Service
  lsof -i :5672   # RabbitMQ AMQP
  lsof -i :15672  # RabbitMQ Management
  lsof -i :3306   # MySQL Order DB
  lsof -i :3307   # MySQL Inventory DB
  lsof -i :3308   # MySQL Payment DB
  ```

- [ ] **磁盘空间** ≥ 2GB (Docker镜像)

- [ ] **网络** - 互联网连接（拉取Docker镜像）

---

## 文件完整性检查

### 项目根目录

- [ ] `pom.xml` ✅
- [ ] `docker-compose.yml` ✅
- [ ] `.gitignore` ✅
- [ ] `README.md` ✅
- [ ] `PROJECT_README.md` ✅
- [ ] `QUICK_START.md` ✅
- [ ] `ARCHITECTURE.md` ✅
- [ ] `COMPLETION_SUMMARY.md` ✅
- [ ] `Postman_Collection.json` ✅

### Order Service (`order-service/`)

- [ ] `pom.xml` ✅
- [ ] `Dockerfile` ✅
- [ ] `src/main/java/com/uwindsor/ecommerce/order/`
  - [ ] `OrderServiceApplication.java` ✅
  - [ ] `controller/OrderController.java` ✅
  - [ ] `controller/DashboardController.java` ✅
  - [ ] `service/OrderService.java` ✅
  - [ ] `service/OrderEventListener.java` ✅
  - [ ] `entity/Order.java` ✅
  - [ ] `entity/SagaLog.java` ✅
  - [ ] `repository/OrderRepository.java` ✅
  - [ ] `repository/SagaLogRepository.java` ✅
  - [ ] `event/OrderCreatedEvent.java` ✅
  - [ ] `event/InventoryReservedEvent.java` ✅
  - [ ] `event/InventoryReservationFailedEvent.java` ✅
  - [ ] `dto/CreateOrderRequest.java` ✅
  - [ ] `dto/CreateOrderResponse.java` ✅
  - [ ] `dto/OrderItemDTO.java` ✅
  - [ ] `config/RabbitMQConfig.java` ✅
- [ ] `src/main/resources/`
  - [ ] `application.yml` ✅
  - [ ] `schema.sql` ✅
  - [ ] `templates/dashboard.html` ✅

### Inventory Service (`inventory-service/`)

- [ ] `pom.xml` ✅
- [ ] `Dockerfile` ✅
- [ ] `src/main/java/com/uwindsor/ecommerce/inventory/`
  - [ ] `InventoryServiceApplication.java` ✅
  - [ ] `service/InventoryService.java` ✅
  - [ ] `service/InventoryEventListener.java` ✅
  - [ ] `entity/Inventory.java` ✅
  - [ ] `entity/InventoryLog.java` ✅
  - [ ] `repository/InventoryRepository.java` ✅
  - [ ] `repository/InventoryLogRepository.java` ✅
  - [ ] `event/InventoryReservedEvent.java` ✅
  - [ ] `event/InventoryReservationFailedEvent.java` ✅
  - [ ] `dto/OrderCreatedEventDTO.java` ✅
  - [ ] `config/RabbitMQConfig.java` ✅
- [ ] `src/main/resources/`
  - [ ] `application.yml` ✅
  - [ ] `schema.sql` ✅

### Payment Service (`payment-service/`)

- [ ] `pom.xml` ✅
- [ ] `Dockerfile` ✅
- [ ] `src/main/java/com/uwindsor/ecommerce/payment/`
  - [ ] `PaymentServiceApplication.java` ✅
  - [ ] `controller/PaymentController.java` ✅
  - [ ] `service/PaymentService.java` ✅
  - [ ] `entity/Payment.java` ✅
  - [ ] `repository/PaymentRepository.java` ✅
  - [ ] `event/PaymentReservedEvent.java` ✅
  - [ ] `event/PaymentReservationFailedEvent.java` ✅
  - [ ] `config/RabbitMQConfig.java` ✅
- [ ] `src/main/resources/`
  - [ ] `application.yml` ✅
  - [ ] `schema.sql` ✅

---

## 代码质量检查

### Order Service

- [ ] 所有类都有JavaDoc注释
- [ ] 异常处理完整
- [ ] 日志记录充分
  ```bash
  grep -r "@Slf4j" order-service/src/main/java/
  ```
- [ ] 事务管理正确
  ```bash
  grep -r "@Transactional" order-service/src/main/java/
  ```

### Inventory Service

- [ ] 所有类都有JavaDoc注释
- [ ] 异常处理完整
- [ ] 日志记录充分
  ```bash
  grep -r "@Slf4j" inventory-service/src/main/java/
  ```
- [ ] 事务管理正确
  ```bash
  grep -r "@Transactional" inventory-service/src/main/java/
  ```

### Payment Service

- [ ] 所有类都有JavaDoc注释
- [ ] 异常处理完整
- [ ] 日志记录充分
  ```bash
  grep -r "@Slf4j" payment-service/src/main/java/
  ```
- [ ] 事务管理正确
  ```bash
  grep -r "@Transactional" payment-service/src/main/java/
  ```

---

## 启动流程检查

### 前置检查

- [ ] 进入项目目录

  ```bash
  cd /Users/logcabin/Workspace/uwindsor/SRS_Ecommerce_Saga
  ```

- [ ] Maven已安装

  ```bash
  mvn --version  # 应返回 3.9.0 或更高
  ```

- [ ] Java SDK已安装
  ```bash
  java -version  # 应返回 17 或更高
  ```

### 启动命令

- [ ] 执行启动命令

  ```bash
  docker compose up --build
  ```

- [ ] 等待所有服务启动完成（30-60秒）
  ```
  Expected output:
  ✓ saga-rabbitmq is running
  ✓ saga-order-db is running
  ✓ saga-inventory-db is running
  ✓ saga-order-service is running
  ✓ saga-inventory-service is running
  ✓ saga-payment-db is running
  ✓ saga-payment-service is running
  ```

---

## 启动后验证

### 服务健康检查

- [ ] RabbitMQ正常

  ```bash
  curl http://localhost:15672 -u guest:guest
  # 预期: 返回RabbitMQ管理页面
  ```

- [ ] Order Service正常

  ```bash
  curl http://localhost:8080/api/orders
  # 预期: 返回空列表 []
  ```

- [ ] Inventory Service正常

  ```bash
  curl http://localhost:8081/
  # 预期: 连接成功 (无特定响应)
  ```

- [ ] Payment Service正常

  ```bash
  curl http://localhost:8083/api/payments/test-order?amount=10&status=SUCCESS -X POST
  # 预期: 返回payment记录JSON
  ```

- [ ] Dashboard正常
  ```bash
  curl http://localhost:8080/dashboard
  # 预期: 返回HTML内容
  ```

### 数据库验证

- [ ] Order DB连接

  ```bash
  mysql -h localhost -P 3306 -u root -proot order_db -e "SELECT 1"
  # 预期: 1
  ```

- [ ] Inventory DB连接

  ```bash
  mysql -h localhost -P 3307 -u root -proot inventory_db -e "SELECT 1"
  # 预期: 1
  ```

- [ ] Payment DB连接

  ```bash
  mysql -h localhost -P 3308 -u root -proot payment_db -e "SELECT 1"
  # 预期: 1
  ```

- [ ] Order DB表检查

  ```bash
  mysql -h localhost -P 3306 -u root -proot order_db -e \
    "SHOW TABLES;"
  # 预期: orders, saga_log
  ```

- [ ] Inventory DB表检查

  ```bash
  mysql -h localhost -P 3307 -u root -proot inventory_db -e \
    "SHOW TABLES;"
  # 预期: inventory, inventory_log
  ```

- [ ] 初始库存数据

  ```bash
  mysql -h localhost -P 3307 -u root -proot inventory_db -e \
    "SELECT * FROM inventory;"
  # 预期: 4种产品 (iPhone 16, MacBook Pro, iPad Air, Apple Watch)
  ```

- [ ] Payment DB表检查

  ```bash
  mysql -h localhost -P 3308 -u root -proot payment_db -e "SHOW TABLES;"
  # 预期: payment
  ```

---

## 功能验证

### 创建订单测试

- [ ] 成功创建订单

  ```bash
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{"userId": 1001, "items": [{"productId": 1, "quantity": 2}]}'
  # 预期: {"orderId": "ORD-XXXXXXXX", "status": "PENDING"}
  ```

- [ ] 查询订单，等待2-5秒后再查

  ```bash
  curl http://localhost:8080/api/orders/ORD-XXXXXXXX
  # 预期: status 从 PENDING → CONFIRMED
  ```

- [ ] 失败订单测试（库存不足）

  ```bash
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{"userId": 1002, "items": [{"productId": 1, "quantity": 10000}]}'
  # 预期: 创建成功，但状态最终为 CANCELLED
  ```

- [ ] 支付失败补偿测试

  ```bash
  curl -X POST "http://localhost:8083/api/payments/ORD-XXXXXXXX?amount=1000&status=FAILED"
  # 预期: order-service收到payment.failed并发布order.cancelled，inventory释放预留库存
  ```

### 仪表板验证

- [ ] 访问仪表板

  ```
  http://localhost:8080/dashboard
  ```

- [ ] 实时更新
  - [ ] 订单列表实时刷新
  - [ ] Saga日志实时刷新
  - [ ] 统计数字更新
  - [ ] 刷新时间戳更新

### 消息队列验证

- [ ] 访问RabbitMQ管理界面

  ```
  http://localhost:15672
  用户: guest
  密码: guest
  ```

- [ ] 检查队列
  - [ ] `order.created.queue`
  - [ ] `order.cancelled.queue`
  - [ ] `inventory.reserved.queue`
  - [ ] `inventory.failed.queue`
  - [ ] `payment.reserved.queue`
  - [ ] `payment.failed.queue`

- [ ] 检查Exchange
  - [ ] `saga.events` (Topic Exchange)

- [ ] 查看消息流
  - [ ] 创建订单后查看消息
  - [ ] 观察消息流向

---

## 日志检查

### Order Service日志

```bash
docker logs saga-order-service -f

# 预期输出:
# - "Order created with ID: ORD-XXXXXXXX"
# - "OrderCreatedEvent published"
# - "Received InventoryReservedEvent"
# - "Order confirmed"
```

### Inventory Service日志

```bash
docker logs saga-inventory-service -f

# 预期输出:
# - "Processing inventory reservation for order"
# - "Inventory reserved for product"
# - "InventoryReservedEvent published"
```

### Payment Service日志

```bash
docker logs saga-payment-service -f

# 预期输出:
# - "Processing payment result"
# - "Payment success event published" 或 "Payment failure event published"
```

### RabbitMQ日志

```bash
docker logs saga-rabbitmq

# 预期: 无错误信息
```

---

## 性能验证

### 响应时间

- [ ] 订单创建 < 200ms
- [ ] 仪表板API响应 < 500ms
- [ ] 端到端Saga完成 < 5秒

### 并发测试

- [ ] 创建多个订单

  ```bash
  for i in {1..5}; do
    curl -X POST http://localhost:8080/api/orders \
      -H "Content-Type: application/json" \
      -d "{\"userId\": $i, \"items\": [{\"productId\": 1, \"quantity\": 1}]}" &
  done
  ```

- [ ] 验证所有订单处理完成

---

## 清理与关闭

### 正常关闭

```bash
# 停止所有容器 (保留数据)
docker compose stop

# 查看容器状态
docker ps -a
```

### 完全清理

```bash
# 停止并删除容器和卷
docker compose down -v

# 删除镜像 (可选)
docker compose down --rmi all
```

---

## 常见问题快速排查

### "连接被拒绝"

- [ ] 确认所有容器运行: `docker ps`
- [ ] 检查端口占用: `lsof -i :PORT`
- [ ] 查看日志: `docker logs SERVICE_NAME`

### "数据库连接超时"

- [ ] 等待容器完全启动 (30秒)
- [ ] 检查健康状态: `docker ps` 看STATUS列
- [ ] 检查网络: `docker network ls`

### "RabbitMQ消息堆积"

- [ ] 访问管理界面清理队列
- [ ] 或执行: `docker exec saga-rabbitmq rabbitmqctl purge_queue QUEUE_NAME`

### "磁盘空间不足"

- [ ] 清理未使用的Docker镜像: `docker image prune`
- [ ] 清理未使用的卷: `docker volume prune`

---

## 部署完成标志

✅ **部署成功的标志**:

1. ✓ 7个容器全部Running
2. ✓ 创建的订单状态自动变化
3. ✓ 仪表板显示实时数据
4. ✓ 没有错误日志
5. ✓ RabbitMQ有消息流动
6. ✓ 数据库中有数据记录

---

## 下一步

部署成功后，建议:

1. 📖 阅读 `QUICK_START.md` 了解测试场景
2. 🧪 使用 Postman Collection 进行API测试
3. 📊 观察仪表板的实时更新
4. 🔍 查看日志理解Saga执行流程
5. 📈 进行压力测试观察性能

---

**检查清单版本**: 1.0  
**最后更新**: 2026年3月23日  
**状态**: ✅ 准备就绪
