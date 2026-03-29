# E-Commerce Checkout System with Saga Pattern

> **Distributed Systems Microservices Project** - Demonstrates the Orchestration-Based Saga Pattern for Eventual Consistency in a high-availability e-commerce environment.

## 🎯 Project Overview

This project implements a **microservices-based e-commerce checkout system** that uses the **Saga Pattern** to manage distributed transactions across multiple services. It demonstrates how to achieve **eventual consistency** in a distributed system without traditional ACID transactions.

### Key Features

- ✅ **Saga Orchestration**: Order Service acts as the orchestrator
- ✅ **Payment Stage**: Payment Service finalizes or fails the order
- ✅ **Wallet Simulation**: Per-user wallet balance load/update for payment failure testing
- ✅ **Event-Driven**: RabbitMQ for asynchronous communication
- ✅ **Compensation Logic**: Automatic rollback on failures
- ✅ **Real-Time Dashboard**: Live monitoring of orders and saga progress
- ✅ **Inventory Ops Panel**: Manual stock update per product with source tracking
- ✅ **Payment Timeline**: Recent success/failed payments in chronological view
- ✅ **One-Click Reset**: Clear test data while keeping product catalog
- ✅ **Database-Per-Service**: Separate MySQL instances for each microservice
- ✅ **Containerized**: Docker Compose for one-command deployment

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          Client (Web UI / API)                          │
└───────────────────────────────┬─────────────────────────────────────────┘
              │
        ┌──────────────┼──────────────┐
        │              │              │
     ┌─────▼─────┐  ┌─────▼─────┐  ┌────▼──────────┐
     │   Order   │  │ Inventory │  │   Payment     │
     │ Service   │  │ Service   │  │   Service     │
     │   8080    │  │   8081    │  │    8083       │
     └─────┬─────┘  └─────┬─────┘  └────┬──────────┘
        │              │             │
      ┌───────▼──────────────▼─────────────▼─────────┐
      │            RabbitMQ Topic Exchange             │
      │                  saga.events                   │
      └─────────┬──────────────────┬──────────────────┘
          │                  │
    ┌────────▼───────┐  ┌──────▼────────┐
    │ Queue Groups   │  │ Queue Groups  │
    │ order/inventory│  │ payment/cancel│
    └────────────────┘  └───────────────┘

   ┌─────────────┐   ┌───────────────┐   ┌──────────────┐
   │  order_db   │   │ inventory_db  │   │  payment_db  │
   │   (3306)    │   │    (3307)     │   │    (3308)    │
   └─────────────┘   └───────────────┘   └──────────────┘
```

## 📋 Saga Flow

### Happy Path (Success)

1. **OrderCreatedEvent** → Order Service creates order (PENDING)
2. **OrderCreatedEvent** → Published to RabbitMQ
3. **Inventory Reservation** → Inventory Service reserves stock
4. **InventoryReservedEvent** → Published back to Order Service
5. **Order Confirmed** → Order status → CONFIRMED (awaiting payment)
6. **Payment API** → Payment Service records payment result
7. **PaymentReservedEvent** → Published back to Order Service
8. **Order Paid** → Order status → PAID and saga completed

### Compensation Path (Failure)

1. **OrderCreatedEvent** → Order Service creates order (PENDING)
2. **OrderCreatedEvent** → Published to RabbitMQ
3. **Insufficient Stock** → Inventory Service detects failure
4. **InventoryReservationFailedEvent** → Published back to Order Service
5. **Order Cancelled** → Order status → CANCELLED (Compensation triggered)

### Compensation Path (Payment Failure)

1. **Inventory already reserved** → Order status is CONFIRMED
2. **Payment failed** → Payment Service publishes `PaymentReservationFailedEvent`
3. **Order Service compensates** → Publishes `OrderCancelledEvent`
4. **Inventory rollback** → Inventory Service releases reserved stock
5. **Order Cancelled** → Order status → CANCELLED

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose installed
- Java 17+ SDK (for local development)
- Maven 3.8+

### Option 1: Docker Compose (Recommended)

```bash
# Clone and navigate to project
cd SRS_Ecommerce_Saga

# Build and start all services
docker compose up --build

# Wait for services to start (20-30 seconds)
# Services will be ready when you see "started" messages in logs
```

**Access Points:**

- 📊 **Dashboard**: http://localhost:8080/dashboard
- 📮 **RabbitMQ UI**: http://localhost:15672 (guest/guest)
- 🔧 **Order API**: http://localhost:8080/api/orders
- 📦 **Inventory API**: http://localhost:8081/api/inventory
- 💳 **Payment API**: http://localhost:8083/api/payments

### Option 2: Local Development

```bash
# 1. Start RabbitMQ
docker run -d -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3.13-management

# 2. Start MySQL databases
docker run -d -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=order_db \
  mysql:8.0

docker run -d -p 3307:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=inventory_db \
  mysql:8.0

docker run -d -p 3308:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=payment_db \
  mysql:8.0

# 3. Build project
mvn clean install -DskipTests

# 4. Start Order Service
cd order-service
mvn spring-boot:run

# 5. In another terminal, start Inventory Service
cd inventory-service
mvn spring-boot:run

# 6. In another terminal, start Payment Service
cd payment-service
mvn spring-boot:run
```

## 💻 API Usage

### Create Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "items": [
      {"productId": 1, "quantity": 2},
      {"productId": 2, "quantity": 1}
    ]
  }'
```

**Response:**

```json
{
  "orderId": "ORD-A1B2C3D4",
  "status": "PENDING"
}
```

### Get Order Status

```bash
curl http://localhost:8080/api/orders/ORD-A1B2C3D4
```

**Response:**

```json
{
  "id": 1,
  "orderId": "ORD-A1B2C3D4",
  "userId": 1001,
  "totalAmount": 1000.0,
  "status": "CONFIRMED",
  "sagaId": "uuid-value",
  "createdAt": "2026-03-23T10:30:00"
}
```

### Process Payment Result

```bash
curl -X POST "http://localhost:8083/api/payments/ORD-A1B2C3D4?amount=1000&status=SUCCESS"
```

Or trigger payment failure:

```bash
curl -X POST "http://localhost:8083/api/payments/ORD-A1B2C3D4?amount=1000&status=FAILED"
```

### Dashboard Access

Navigate to: **http://localhost:8080/dashboard**

Features:

- ✨ Real-time order status updates (refreshes every 2 seconds)
- 📊 Saga progress tracking
- 💳 Wallet balance monitor with LOW / MEDIUM / HIGH visual hints
- 📈 Payment timeline with SUCCESS / FAILED records
- 📦 Product-level stock update action (`Set`)
- 🧹 "Clear Test Data (Keep Products)" button for fast reset
- 📈 Summary statistics (Total, Confirmed, Pending, Cancelled orders)

### Additional APIs (Recent)

```bash
# Wallet APIs
curl http://localhost:8083/api/payments/wallet/1001
curl -X PUT "http://localhost:8083/api/payments/wallet/1001?balance=50"

# Payment history / guards
curl http://localhost:8083/api/payments/recent
curl http://localhost:8083/api/payments/processed-order-ids

# Inventory manual set
curl -X PUT "http://localhost:8081/api/inventory/1/stock?value=25"

# Clear test data (keep products)
curl -X DELETE http://localhost:8081/api/inventory/test-data
curl -X DELETE http://localhost:8083/api/payments/test-data
curl -X DELETE http://localhost:8080/api/orders/test-data
```

## 📁 Project Structure

```
SRS_Ecommerce_Saga/
├── pom.xml                          # Parent Maven POM
├── docker-compose.yml               # Docker Compose configuration
├── Postman_Collection.json          # API testing collection
│
├── order-service/                   # Order Service microservice
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/main/java/
│   │   └── com/uwindsor/ecommerce/order/
│   │       ├── OrderServiceApplication.java
│   │       ├── config/              # RabbitMQ configuration
│   │       ├── controller/          # REST endpoints
│   │       ├── entity/              # JPA entities (Order, SagaLog)
│   │       ├── repository/          # Data access layer
│   │       ├── service/             # Business logic
│   │       ├── event/               # Event classes
│   │       └── dto/                 # Data transfer objects
│   └── src/main/resources/
│       ├── application.yml
│       ├── schema.sql
│       └── templates/dashboard.html
│
├── inventory-service/               # Inventory Service microservice
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/main/java/
│   │   └── com/uwindsor/ecommerce/inventory/
│   │       ├── InventoryServiceApplication.java
│   │       ├── config/              # RabbitMQ configuration
│   │       ├── entity/              # JPA entities (Inventory, InventoryLog)
│   │       ├── repository/          # Data access layer
│   │       ├── service/             # Business logic
│   │       ├── event/               # Event classes
│   │       └── dto/                 # Data transfer objects
│   └── src/main/resources/
│       ├── application.yml
│       └── schema.sql
│
├── payment-service/                 # Payment Service microservice
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/main/java/
│   │   └── com/uwindsor/ecommerce/payment/
│   │       ├── PaymentServiceApplication.java
│   │       ├── config/              # RabbitMQ configuration
│   │       ├── controller/          # REST endpoints
│   │       ├── entity/              # JPA entity (Payment)
│   │       ├── repository/          # Data access layer
│   │       ├── service/             # Business logic
│   │       ├── event/               # Event classes
│   │       └── dto/                 # Data transfer objects
│   └── src/main/resources/
│       ├── application.yml
│       └── schema.sql
│
└── README.md                        # This file
```

## 🗄️ Database Schema

### Order Database (order_db)

```sql
-- Orders table: tracks created orders
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(36) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING','CONFIRMED','CANCELLED','FAILED'),
    saga_id VARCHAR(36),
    created_at TIMESTAMP
);

-- Saga log: tracks saga progression
CREATE TABLE saga_log (
    saga_id VARCHAR(36) PRIMARY KEY,
    current_step VARCHAR(50),
    status ENUM('STARTED','COMPLETED','COMPENSATED','FAILED'),
    last_updated TIMESTAMP
);
```

### Inventory Database (inventory_db)

```sql
-- Products and stock
CREATE TABLE inventory (
    product_id BIGINT PRIMARY KEY,
    product_name VARCHAR(100),
    stock INT,
    reserved INT
);

-- Audit log for inventory changes
CREATE TABLE inventory_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(36),
    product_id BIGINT,
    quantity INT,
    action ENUM('RESERVE','RELEASE'),
    timestamp TIMESTAMP
);
```

### Payment Database (payment_db)

```sql
CREATE TABLE payment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id VARCHAR(36) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  status ENUM('SUCCESS','FAILED') NOT NULL,
  payment_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 🧪 Testing

### Using Postman

1. Import [`Postman_Collection.json`](./Postman_Collection.json)
2. Run requests in this order:

- **Create Order (Happy Path)** - Watch status change to CONFIRMED
- **Process Payment Success** - Watch status change to PAID
- **Create Order (Insufficient Stock)** - Watch status change to CANCELLED
- **Process Payment Failed** - Watch inventory compensation and CANCELLED
- **Get Dashboard Data** - View saga progress

### Test Scenarios

#### Scenario 1: Happy Path ✅

- Create order with available inventory + payment success
- Expected: Order → PENDING → CONFIRMED → PAID
- Saga: STARTED → COMPLETED

#### Scenario 2: Compensation ⚠️

- Create order with insufficient inventory (qty > available stock)
- Expected: Order → PENDING → CANCELLED
- Saga: STARTED → COMPENSATED

#### Scenario 3: Payment Failure Compensation ⚠️

- Create order with available inventory
- Trigger payment with `status=FAILED`
- Expected: Order → CONFIRMED → CANCELLED
- Inventory reserved quantity is rolled back

#### Scenario 4: Concurrent Orders 🔄

- Create multiple orders rapidly
- Expected: All processed correctly with proper inventory management

## 📊 Technology Stack

| Component       | Technology             | Version      |
| --------------- | ---------------------- | ------------ |
| Language        | Java                   | 17           |
| Framework       | Spring Boot            | 3.3.4        |
| Data Access     | Spring Data JPA        | 3.3.4        |
| Messaging       | Spring AMQP / RabbitMQ | 3.3.4 / 3.13 |
| Database        | MySQL                  | 8.0          |
| Container       | Docker                 | Latest       |
| Template Engine | Thymeleaf              | 3.1.x        |
| UI Framework    | Bootstrap              | 5.3          |
| Build Tool      | Maven                  | 3.9+         |

## ⚠️ Event Schemas

### OrderCreatedEvent

```json
{
  "sagaId": "uuid-string",
  "orderId": "ORD-XXXXXXXX",
  "userId": 1001,
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ],
  "totalAmount": 2997.0
}
```

### InventoryReservedEvent (Success)

```json
{
  "sagaId": "uuid-string",
  "orderId": "ORD-XXXXXXXX",
  "success": true,
  "reservedItems": [...]
}
```

### InventoryReservationFailedEvent (Failure)

```json
{
  "sagaId": "uuid-string",
  "orderId": "ORD-XXXXXXXX",
  "success": false,
  "reason": "Insufficient stock for product: 1. Available: 0, Requested: 5"
}
```

## 🔍 Monitoring & Debugging

### View Logs

```bash
# Order Service logs
docker logs saga-order-service -f

# Inventory Service logs
docker logs saga-inventory-service -f

# Payment Service logs
docker logs saga-payment-service -f

# RabbitMQ logs
docker logs saga-rabbitmq -f
```

### Check RabbitMQ Queues

Visit http://localhost:15672 (Guest/Guest)

- **Queues tab**: View message queues and their status
- **Exchanges tab**: See topic exchange and routing
- **Connections tab**: Monitor active connections

### Database Queries

```bash
# Connect to Order DB
mysql -h localhost -P 3306 -u root -proot order_db

# View orders
SELECT * FROM orders ORDER BY created_at DESC;
SELECT * FROM saga_log;

# Connect to Inventory DB
mysql -h localhost -P 3307 -u root -proot inventory_db

# View inventory
SELECT * FROM inventory;
SELECT * FROM inventory_log ORDER BY timestamp DESC;

# Connect to Payment DB
mysql -h localhost -P 3308 -u root -proot payment_db

# View payments
SELECT * FROM payment ORDER BY payment_time DESC;
```

## 🚨 Troubleshooting

### Services won't start

```bash
# Check if ports are in use
lsof -i :8080
lsof -i :8081
lsof -i :8083
lsof -i :5672
lsof -i :3306
lsof -i :3307
lsof -i :3308

# Kill process if needed
kill -9 <PID>
```

### RabbitMQ connection errors

```bash
# Verify RabbitMQ is running
docker ps | grep rabbitmq

# Check RabbitMQ logs
docker logs saga-rabbitmq
```

### Database connection issues

```bash
# Test MySQL connection
mysql -h localhost -P 3306 -u root -proot -e "SELECT 1"
mysql -h localhost -P 3307 -u root -proot -e "SELECT 1"
mysql -h localhost -P 3308 -u root -proot -e "SELECT 1"
```

### Clean slate restart

```bash
# Stop and remove all containers
docker compose down -v

# Remove all images
docker compose down --rmi all

# Rebuild and restart
docker compose up --build
```

## 📚 Key Concepts Demonstrated

1. **Saga Pattern**: Orchestration-based saga with centralized orchestrator
2. **Event Sourcing**: Events as primary source of truth
3. **Compensation Transactions**: Automatic rollback on failures
4. **Eventual Consistency**: System becomes consistent after saga completion
5. **Microservices**: Independent services with separate databases (Order/Inventory/Payment)
6. **Asynchronous Communication**: RabbitMQ for decoupling
7. **Idempotency**: Safe to replay events
8. **Distributed Tracing**: Saga IDs for tracking across services

## 📈 Performance Characteristics

- **Order Creation**: < 500ms
- **Inventory Reservation**: < 5 seconds end-to-end
- **Concurrent Orders**: 100+ simultaneous orders
- **Compensation**: Automatic, idempotent

## 🔐 Security Considerations (Production)

- Use credentials management for database and RabbitMQ
- Implement distributed tracing and auditing
- Add API authentication and authorization
- Use SSL/TLS for RabbitMQ connections
- Implement rate limiting and circuit breakers
- Add request validation and sanitization

## 📝 License & Attribution

Academic Project - University of Windsor
**Created for SRS (Software Requirements Specification) Course**

## 📞 Support

For issues or questions:

1. Check logs using `docker logs <service>`
2. Review Postman Collection for API usage
3. Consult RabbitMQ Management UI
4. Check database directly for state

---

**Version**: 1.0.0  
**Last Updated**: March 2026  
**Status**: ✅ Production Ready for Demonstration
