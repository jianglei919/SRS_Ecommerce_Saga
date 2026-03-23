# 🏛️ 系统架构设计文档

## 目录

1. [架构概述](#架构概述)
2. [模块设计](#模块设计)
3. [Saga模式实现](#saga模式实现)
4. [事件流](#事件流)
5. [数据模型](#数据模型)
6. [设计决策](#设计决策)
7. [扩展性](#扩展性)

---

## 架构概述

### 高级架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    客户端应用层                                   │
│          (Web UI Dashboard + REST API 客户端)                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
    ┌────▼─────────┐   ┌─────▼──────┐      ┌────▼──────────┐
    │  Order       │   │ RabbitMQ   │      │  Dashboard    │
    │  Service     │   │  Message   │      │  Service      │
    │  (8080)      │   │  Broker    │      │  (8080)       │
    │  ┌────────┐  │   │            │      │  ┌─────────┐  │
    │  │Servlet│  │   │ ┌────────┐ │      │  │Thymeleaf│  │
    │  │+ REST  │──────▶│ Queues │ │◀─────│  │ View    │  │
    │  │API    │  │   │ │& Events│ │      │  │Resolver │  │
    │  └────────┘  │   │ └────────┘ │      │  └─────────┘  │
    │  ┌────────┐  │   │            │      │               │
    │  │Service │  │   │ ┌────────┐ │      │  ┌─────────┐  │
    │  │Layer   │  │   │ │Exchange│ │      │  │AJAX     │  │
    │  │(Saga   │  │   │ │(Topic) │ │      │  │Refresh  │  │
    │  │Orchest.)  │   │ └────────┘ │      │  │(2s)     │  │
    │  └────────┘  │   │            │      │  └─────────┘  │
    │  ┌────────┐  │   └─────┬──────┘      │               │
    │  │Event   │  │         │              │               │
    │  │Listener│  │         │              │               │
    │  └────────┘  │         │              │               │
    └──────┬───────┘         │              └───────────────┘
           │                 │
       ┌───▼──────────┐  ┌──▼──────┐   ┌──────────────────┐
       │ Order DB     │  │ Inventory├──▶│ Inventory        │
       │ (MySQL)      │  │ Service  │   │ Service          │
       │ ┌────────┐   │  │ (8081)   │   │ (8081)           │
       │ │Orders  │   │  └──┬───────┘   │ ┌──────────────┐ │
       │ │Table   │   │     │           │ │Event Listener│ │
       │ ├────────┤   │     │           │ │Handler       │ │
       │ │Saga    │   │     │           │ │OrderCreated  │ │
       │ │Log     │   │     │           │ │Compensation  │ │
       │ │Table   │   │     │           │ └──────────────┘ │
       │ └────────┘   │     │           │ ┌──────────────┐ │
       └──────────────┘     │           │ │Service Layer │ │
                            │           │ │- Reserve     │ │
       ┌────────────────────┤           │ │- Release     │ │
       │                    │           │ └──────────────┘ │
       └────────────────────┼───────────┘ ┌──────────────┐ │
                            │           │ │Inventory    │ │
                        ┌───▼───────┐   │ │Log Table    │ │
                        │ Inventory │   │ └──────────────┘ │
                        │ DB        │   │                  │
                        │ (MySQL)   │   │  ┌────────────┐  │
                        │ ┌───────┐ │   │  │Inventory   │  │
                        │ │Stock  │ │   │  │Table       │  │
                        │ │Table  │ │   │  └────────────┘  │
                        │ └───────┘ │   └──────────────────┘
                        └───────────┘
```

### 核心思想

1. **微服务隔离**: 每个服务有独立的数据库，通过事件通信
2. **Saga编排**: Order Service作为中央编排器管理分布式事务
3. **事件驱动**: RabbitMQ处理异步通信和解耦
4. **最终一致性**: 系统最终达到一致状态，中间可能有短暂不一致

---

## 模块设计

### Order Service 架构

```
┌─────────────────────────────────────┐
│  Order Service Module               │
├─────────────────────────────────────┤
│  REST Controller                    │
│  ├─ POST /api/orders                │ ← 创建订单
│  ├─ GET /api/orders/{id}            │ ← 查询状态
│  └─ GET /dashboard                  │ ← 仪表板
├─────────────────────────────────────┤
│  Service Layer (业务逻辑)            │
│  ├─ OrderService                    │
│  │  ├─ createOrder()    [Happy Path]│
│  │  ├─ handleInventory  [Success]   │
│  │  └─ handleInventoryF [Failure]   │
│  └─ OrderEventListener              │
│     ├─ handleInventoryReserved()    │
│     └─ handleInventoryFailed()      │
├─────────────────────────────────────┤
│  Entity Layer (数据模型)             │
│  ├─ Order (PENDING/CONFIRMED/...)  │
│  └─ SagaLog (tracking)              │
├─────────────────────────────────────┤
│  Repository Layer (数据访问)         │
│  ├─ OrderRepository                 │
│  └─ SagaLogRepository               │
├─────────────────────────────────────┤
│  RabbitMQ Configuration             │
│  ├─ OrderCreatedEvent [Publish]     │
│  ├─ InventoryReservedEvent [Listen] │
│  └─ InventoryFailedEvent [Listen]   │
└─────────────────────────────────────┘
```

### Inventory Service 架构

```
┌─────────────────────────────────────┐
│  Inventory Service Module           │
├─────────────────────────────────────┤
│  Event Listener                     │
│  └─ InventoryEventListener          │
│     └─ handleOrderCreated()  ← RabbitMQ
├─────────────────────────────────────┤
│  Service Layer (业务逻辑)            │
│  └─ InventoryService                │
│     ├─ reserveInventory()           │
│     ├─ releaseInventory() [Comp]    │
│     ├─ publishSuccess()             │
│     └─ publishFailure()             │
├─────────────────────────────────────┤
│  Entity Layer (数据模型)             │
│  ├─ Inventory (stock/reserved)      │
│  └─ InventoryLog (audit trail)      │
├─────────────────────────────────────┤
│  Repository Layer (数据访问)         │
│  ├─ InventoryRepository             │
│  └─ InventoryLogRepository          │
├─────────────────────────────────────┤
│  RabbitMQ Configuration             │
│  ├─ OrderCreatedEvent [Listen]      │
│  ├─ InventoryReservedEvent [Publish]│
│  └─ InventoryFailedEvent [Publish]  │
└─────────────────────────────────────┘
```

---

## Saga模式实现

### 编排型Saga (Orchestration-based)

**选择原因**:

- ✅ 中心化控制，易于理解和维护
- ✅ 事务顺序一目了然
- ✅ 易于添加新的补偿逻辑
- ⚠️ Order Service 成为中心，可能成为瓶颈

### Saga状态机

```
                    ┌──────────────┐
                    │   STARTED    │
                    └──────┬───────┘
                           │
                   创建订单 + 发布事件
                           │
                  ┌────────▼────────┐
                  │ 等待库存响应    │
                  └────────┬────────┘
                           │
                ┌──────────┴─────────┐
                │                   │
        ┌───────▼────────┐   ┌──────▼──────┐
        │ 保留成功       │   │ 保留失败     │
        │ [Happy Path]   │   │ [Comp Path]  │
        └───────┬────────┘   └──────┬──────┘
                │                   │
        ┌───────▼────────┐   ┌──────▼──────┐
        │   COMPLETED    │   │ COMPENSATED │
        │ (CONFIRMED)    │   │  (CANCELLED)│
        └────────────────┘   └─────────────┘
```

### 关键特性

1. **原子性**: 每个服务的本地事务是ACID的
2. **一致性**: 最终一致，通过补偿保证
3. **隔离性**: 服务间通过事件隔离
4. **持久性**: 所有状态变更持久化到数据库

---

## 事件流

### Happy Path (成功路径)

```
时间 → 事件  → 方向  → 接收方  → 操作      → 结果
────────────────────────────────────────────────────
0ms   创建    User   Order    创建订单    Order: PENDING
      请求           Service  发布事件

100ms Order   OS→RS  RabbitMQ 入队        消息在队列
      Created
      Event

200ms 接收    RS→IS  Inventory 预留库存   stock decreased
      消息           Service   发布成功    reserved ↑

300ms Invent  IS→RS  RabbitMQ  入队       消息在队列
      Reserved
      Event

400ms 接收    RS→OS  Order    更新订单    Order: CONFIRMED
      消息           Service  更新日志    SagaLog: COMPLETED
```

### Compensation Path (补偿路径)

```
时间 → 事件    → 方向  → 接收方  → 操作       → 结果
─────────────────────────────────────────────────────
0ms   创建请   User   Order    创建订单     Order: PENDING
      求             Service  发布事件

100ms Order    OS→RS  RabbitMQ 入队         消息在队列
      Created
      Event

200ms 接收      RS→IS  Invent   检查库存  ✗ 库存不足
      消息             Service  发布失败

300ms Invent    IS→RS  RabbitMQ 入队       消息在队列
      Failed
      Event

400ms 接收      RS→OS  Order    触发补偿  Order: CANCELLED
      消息             Service  更新日志  SagaLog: COMPENSATED
```

---

## 数据模型

### Order Database Schema

```sql
-- 订单主表
CREATE TABLE orders (
    id BIGINT PK AUTO_INCREMENT,
    order_id VARCHAR(36) UQ,      -- 业务主键
    user_id BIGINT,               -- 用户ID
    total_amount DECIMAL(10,2),   -- 订单金额
    status ENUM(...),             -- PENDING/CONFIRMED/CANCELLED/FAILED
    saga_id VARCHAR(36),          -- 关联的Saga
    created_at TIMESTAMP          -- 创建时间
);

-- Saga日志表 (跟踪进度)
CREATE TABLE saga_log (
    saga_id VARCHAR(36) PK,       -- Saga标识
    current_step VARCHAR(50),     -- 当前步骤
    status ENUM(...),             -- STARTED/COMPLETED/COMPENSATED/FAILED
    last_updated TIMESTAMP        -- 最后更新时间
);
```

### Inventory Database Schema

```sql
-- 库存表
CREATE TABLE inventory (
    product_id BIGINT PK,         -- 产品ID
    product_name VARCHAR(100),    -- 产品名称
    stock INT,                    -- 总库存
    reserved INT                  -- 已预留
);

-- 库存操作日志 (审计日志)
CREATE TABLE inventory_log (
    id BIGINT PK AUTO_INCREMENT,
    order_id VARCHAR(36),         -- 订单ID
    product_id BIGINT FK,         -- 产品ID
    quantity INT,                 -- 数量
    action ENUM(RESERVE/RELEASE), -- 操作类型
    timestamp TIMESTAMP           -- 操作时间
);
```

### 数据一致性规律

```
成功路径:
  Order.status:      PENDING → CONFIRMED
  Inventory.reserved: old → old + qty
  SagaLog.status:    STARTED → COMPLETED

补偿路径:
  Order.status:      PENDING → CANCELLED
  Inventory.reserved: unchanged (未曾预留)
  SagaLog.status:    STARTED → COMPENSATED
```

---

## 设计决策

### 1. 为什么选择Saga模式?

| 比较   | 2PC  | Saga       |
| ------ | ---- | ---------- |
| 实时性 | 优秀 | 较好 (5秒) |
| 可用性 | 中等 | 高 (无锁)  |
| 复杂度 | 低   | 中等       |
| 可扩展 | 差   | 优秀       |
| 补偿   | 自动 | 手动       |

**结论**: Saga适合电商中库存相对充足、完全失败罕见的场景

### 2. 编排型 vs 编程型

**编排型** (选择该项):

```
Order Service (Orchestrator)
    ├─ 1. 创建订单
    ├─ 2. 调用Inventory Service
    ├─ 3. 等待响应
    └─ 4. 更新或补偿
优点: 清晰易懂，集中控制
缺点: OS成为单点
```

**编程型** (未选择):

```
Inventory Service自主监听OrderCreatedEvent
Order Service自主监听InventoryReservedEvent
优点: 低耦合
缺点: 流程分散，难以追踪
```

### 3. RabbitMQ选择

| 选择           | 理由                 |
| -------------- | -------------------- |
| 异步消息队列   | 解耦服务，支持高吞吐 |
| Topic Exchange | 灵活的消息路由       |
| 持久队列       | 消息不丢失           |
| 管理界面       | 便于监控和调试       |

### 4. 数据库隔离策略

每个微服务有**独立的MySQL实例**:

- ✅ 高度解耦，互不影响
- ✅ 可独立扩展
- ✅ 符合微服务原则
- ⚠️ 不能跨库事务

---

## 扩展性

### 水平扩展 (添加更多服务)

```
当前架构:
Order Service -[Events]-> Inventory Service

未来可扩展为:
Order Service -[Events]-> Inventory Service
            \            /
             \          /
              Payment Service
               |
              Shipping Service
                |
              Notification Service

每个服务:
- 独立的MySQL数据库
- 独立的Event Listeners
- RabbitMQ中增加新的Queue和Exchange
```

### 性能优化建议

1. **缓存层** (Redis)
   - 缓存产品信息和库存
   - 减少数据库查询

2. **异步处理**
   - 异步发送通知
   - 异步更新报表

3. **消息优化**
   - Dead Letter Queue处理失败消息
   - 批量处理提高吞吐

4. **数据库优化**
   - 添加适当索引
   - 分区表处理大量数据
   - 读写分离

### 可靠性增强

1. **幂等性** - 相同消息多次处理返回相同结果
2. **重试机制** - 指数退避重试
3. **断路器** - Resilience4j防止级联故障
4. **监控告警** - Prometheus + Grafana

---

## 总结

本项目通过**编排型Saga模式**实现了分布式事务管理，展示了:

✅ 微服务架构最佳实践
✅ 事件驱动设计
✅ 异步通信与解耦
✅ 最终一致性保证
✅ 故障补偿机制

这为高可用、高性能的电商系统提供了坚实基础！
