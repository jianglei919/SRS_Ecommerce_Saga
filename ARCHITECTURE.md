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
┌─────────────────────────────────────────────────────────────────────────┐
│                          客户端应用层                                   │
│                 (Web UI Dashboard + REST API Client)                    │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
            ┌───────────────────┼───────────────────┐
            │                   │                   │
      ┌─────▼─────┐      ┌──────▼──────┐      ┌────▼───────┐
      │  Order    │      │ Inventory   │      │  Payment   │
      │ Service   │      │ Service     │      │  Service   │
      │   8080    │      │   8081      │      │   8083     │
      └─────┬─────┘      └──────┬──────┘      └────┬───────┘
            │                   │                   │
      ┌─────▼───────────────────▼───────────────────▼───────┐
      │                    RabbitMQ (AMQP)                   │
      │               Exchange: saga.events                  │
      └─────┬───────────────────┬───────────────────┬───────┘
            │                   │                   │
  ┌─────────▼─────────┐ ┌───────▼─────────┐ ┌──────▼────────────┐
  │ order.created.queue│ │inventory.*.queue│ │payment.*.queue    │
  │ order.cancelled.q  │ │                 │ │                    │
  └────────────────────┘ └─────────────────┘ └───────────────────┘

      ┌──────────────┐      ┌───────────────┐      ┌──────────────┐
      │  order_db    │      │ inventory_db  │      │ payment_db   │
      │  MySQL 3306  │      │  MySQL 3307   │      │ MySQL 3308   │
      └──────────────┘      └───────────────┘      └──────────────┘
```

### 核心思想

1. 微服务隔离: 每个服务有独立数据库，通过事件通信。
2. Saga编排: Order Service 作为中央编排器管理分布式事务。
3. 事件驱动: RabbitMQ 负责异步通信与服务解耦。
4. 最终一致性: 系统允许短暂不一致，最终自动收敛。

---

## 模块设计

### Order Service 架构

```
┌─────────────────────────────────────┐
│  Order Service Module               │
├─────────────────────────────────────┤
│  REST Controller                    │
│  ├─ POST /api/orders                │
│  ├─ GET /api/orders/{id}            │
│  └─ GET /dashboard                  │
├─────────────────────────────────────┤
│  Service Layer                      │
│  ├─ createOrder()                   │
│  ├─ handleInventoryReserved()       │
│  ├─ handlePaymentSuccess()          │
│  └─ handlePaymentFailed()           │
├─────────────────────────────────────┤
│  Event Listener                     │
│  ├─ inventory.reserved              │
│  ├─ inventory.failed                │
│  ├─ payment.reserved                │
│  └─ payment.failed                  │
└─────────────────────────────────────┘
```

### Inventory Service 架构

```
┌─────────────────────────────────────┐
│  Inventory Service Module           │
├─────────────────────────────────────┤
│  Event Listener                     │
│  ├─ order.created                   │
│  └─ order.cancelled                 │
├─────────────────────────────────────┤
│  Service Layer                      │
│  ├─ reserveInventory()              │
│  └─ releaseInventory()              │
├─────────────────────────────────────┤
│  Data Model                         │
│  ├─ inventory                       │
│  └─ inventory_log                   │
└─────────────────────────────────────┘
```

### Payment Service 架构

```
┌─────────────────────────────────────┐
│  Payment Service Module             │
├─────────────────────────────────────┤
│  REST Controller                    │
│  ├─ POST /api/payments/{orderId}    │
│  ├─ GET /api/payments/recent        │
│  ├─ GET /api/payments/wallet/{userId}│
│  ├─ PUT /api/payments/wallet/{userId}│
│  └─ DELETE /api/payments/test-data  │
├─────────────────────────────────────┤
│  Service Layer                      │
│  ├─ processPaymentResult()          │
│  ├─ no-duplicate payment guard      │
│  ├─ wallet read/update              │
│  ├─ publish payment.reserved        │
│  └─ publish payment.failed          │
├─────────────────────────────────────┤
│  Data Model                         │
│  ├─ payment                         │
│  └─ wallet                          │
└─────────────────────────────────────┘
```

---

## Saga模式实现

### Saga状态机

```
STARTED
  │
  ├─(order.created)───────────────────────────────┐
  │                                                │
  ▼                                                ▼
WAIT_INVENTORY                               INVENTORY_FAILED
  │                                                │
  ├─ inventory.reserved                            │
  ▼                                                │
AWAITING_PAYMENT                                  │
  │                                                │
  ├─ payment.reserved ─────────────► COMPLETED (PAID)
  │
  └─ payment.failed ─► COMPENSATING_INVENTORY ─► COMPENSATED (CANCELLED)
```

---

## 事件流

### Happy Path (库存成功 + 支付成功)

```
0ms   User -> Order Service: POST /api/orders
50ms  Order Service: create PENDING + publish order.created
150ms Inventory Service: reserve stock + publish inventory.reserved
250ms Order Service: set CONFIRMED + step AWAITING_PAYMENT
400ms User -> Payment Service: POST /api/payments/{orderId}?status=SUCCESS
500ms Payment Service: persist payment + publish payment.reserved
650ms Order Service: set PAID + saga COMPLETED
```

### Compensation Path A (库存失败)

```
0ms   User -> Order Service: POST /api/orders
100ms Inventory Service: detect insufficient stock
150ms Inventory Service: publish inventory.failed
250ms Order Service: set CANCELLED + saga COMPENSATED
```

### Compensation Path B (支付失败，库存已预留)

```
0ms   Inventory already reserved, order CONFIRMED
100ms User -> Payment Service: status=FAILED
150ms Payment Service: publish payment.failed
250ms Order Service: publish order.cancelled
350ms Inventory Service: release reserved stock
450ms Order Service: keep status CANCELLED, saga COMPENSATED
```

---

## 数据模型

### Order Database Schema

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(36) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING','CONFIRMED','PAID','CANCELLED','FAILED'),
    saga_id VARCHAR(36),
    created_at TIMESTAMP
);

CREATE TABLE saga_log (
    saga_id VARCHAR(36) PRIMARY KEY,
    current_step VARCHAR(50),
    status ENUM('STARTED','COMPLETED','COMPENSATED','FAILED'),
    last_updated TIMESTAMP
);
```

### Inventory Database Schema

```sql
CREATE TABLE inventory (
    product_id BIGINT PRIMARY KEY,
    product_name VARCHAR(100),
    stock INT,
    reserved INT
);

CREATE TABLE inventory_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(36),
    product_id BIGINT,
    quantity INT,
  action ENUM('RESERVE','RELEASE','MANUAL_SET'),
    timestamp TIMESTAMP
);
```

### Payment Database Schema

```sql
CREATE TABLE payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM('SUCCESS','FAILED') NOT NULL,
    payment_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wallet (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNIQUE NOT NULL,
  balance DECIMAL(10,2) NOT NULL
);
```

---

## 设计决策

1. 使用 Saga 而非 2PC，降低阻塞风险并提升可用性。
2. 引入 Payment Service，将库存确认与支付确认解耦。
3. 通过 `order.cancelled` 事件驱动库存补偿，保持服务边界清晰。
4. Dashboard 支持支付时间线、钱包等级提示、手动库存变更与测试数据一键清理，便于课堂演示与回归验证。

---

## 扩展性

### 当前架构

```
Order Service --(order.created)--> Inventory Service
Payment Service --(payment.*)-----> Order Service
Order Service --(order.cancelled)-> Inventory Service
```

### 后续可扩展

1. Shipping Service: 仅消费 PAID 订单。
2. Notification Service: 统一发送通知。
3. Fraud Service: 风控拒付触发补偿。

---

## 总结

系统已升级为“订单 + 库存 + 支付”的三阶段 Saga：

- 支持支付成功后订单状态收敛到 `PAID`。
- 支持支付失败后触发库存补偿。
- 保持服务解耦与数据库隔离。
