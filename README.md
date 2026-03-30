# 🛒 Distributed E-Commerce Order Engine

A **production-grade**, fully working distributed e-commerce backend built with **Java 17 + Spring Boot 3 + H2 + JPA/Hibernate + Multithreading**.

---

## 📦 Project Structure

```
ecommerce-order-engine/
├── pom.xml
└── src/main/java/com/ecommerce/
    ├── EcommerceOrderEngineApplication.java   ← Entry point
    ├── model/                                 ← JPA Entities
    │   ├── Product.java
    │   ├── CartItem.java
    │   ├── Order.java
    │   ├── OrderItem.java
    │   ├── OrderStatus.java                   ← State machine enum
    │   ├── AuditLog.java
    │   └── StockReservation.java
    ├── repository/                            ← Spring Data JPA repos
    │   ├── ProductRepository.java
    │   ├── CartItemRepository.java
    │   ├── OrderRepository.java
    │   ├── AuditLogRepository.java
    │   └── StockReservationRepository.java
    ├── service/                               ← Service interfaces (Microservice modules)
    │   ├── ProductService.java
    │   ├── CartService.java
    │   ├── OrderService.java
    │   └── InventoryService.java
    ├── service/impl/                          ← Implementations
    │   ├── ProductServiceImpl.java
    │   ├── CartServiceImpl.java
    │   ├── OrderServiceImpl.java
    │   ├── InventoryServiceImpl.java
    │   └── ConcurrencySimulatorService.java
    ├── event/                                 ← Event-driven system (Observer pattern)
    │   ├── EventType.java
    │   ├── DomainEvent.java
    │   ├── EventListener.java
    │   ├── EventBus.java                      ← In-memory Kafka simulation
    │   ├── OrderCreatedListener.java
    │   ├── PaymentSuccessListener.java
    │   └── InventoryUpdatedListener.java
    ├── payment/                               ← Factory pattern
    │   ├── PaymentProcessor.java
    │   ├── PaymentResult.java
    │   ├── UpiPaymentProcessor.java
    │   ├── CardPaymentProcessor.java
    │   ├── CodPaymentProcessor.java
    │   └── PaymentProcessorFactory.java
    ├── discount/                              ← Strategy pattern
    │   ├── DiscountStrategy.java
    │   └── CompositeDiscountStrategy.java
    ├── audit/
    │   └── AuditLogger.java                   ← Singleton pattern
    ├── cache/
    │   └── InMemoryCache.java                 ← Redis simulation
    ├── lock/
    │   └── LockManager.java                   ← ReentrantLock manager
    ├── fraud/
    │   └── FraudDetectionEngine.java
    ├── idempotency/
    │   └── IdempotencyStore.java
    ├── exception/                             ← Custom exceptions
    │   ├── ProductNotFoundException.java
    │   ├── InsufficientStockException.java
    │   ├── OrderNotFoundException.java
    │   ├── DuplicateProductException.java
    │   ├── PaymentFailedException.java
    │   ├── FraudDetectedException.java
    │   ├── InvalidCouponException.java
    │   ├── OrderCancellationException.java
    │   └── DuplicateOrderException.java
    ├── config/
    │   └── AppConfig.java
    └── cli/
        └── CLIMenuRunner.java                ← Full terminal menu
```

---

## ✨ Features

| # | Feature | Implementation |
|---|---------|----------------|
| 1 | Product Management | Duplicate ID prevention, negative stock guard, cache write-through |
| 2 | Multi-User Cart | Per-user CartItem rows, isolated sessions |
| 3 | Real-Time Stock Reservation | `StockReservation` entity with expiry, ties into Cart add/remove |
| 4 | Concurrency Control | `ReentrantLock` (fair) + `ExecutorService` + `CountDownLatch` |
| 5 | Order Placement Engine | 7-step atomic pipeline (validate → discount → lock → create → pay → deduct) |
| 6 | Payment Simulation | UPI/CARD/COD factories with random failure rates |
| 7 | Transaction Rollback | `@Transactional(rollbackFor=Exception.class)` + manual stock release |
| 8 | Order State Machine | Enum-driven state transitions with guard conditions |
| 9 | Discount & Coupon Engine | Strategy pattern with 4 stacking rules |
| 10 | Inventory Alert System | Low-stock query, out-of-stock block at reservation |
| 11 | Order Management | Filter/search by status, ID lookup, full detail view |
| 12 | Order Cancellation | State guard + stock restore + double-cancel prevention |
| 13 | Return & Refund | Partial return, quantity tracking, refund calculation |
| 14 | Event-Driven System | In-memory EventBus (Kafka simulation), ordered chain, failure stops chain |
| 15 | Reservation Expiry | `@Scheduled` every 30s auto-releases expired stock |
| 16 | Audit Logging | Immutable `AuditLog` entities, `REQUIRES_NEW` tx survives rollbacks |
| 17 | Fraud Detection | Rate limiting (3 orders/min) + high-value flagging |
| 18 | Failure Injection | Global toggle on `PaymentProcessorFactory` |
| 19 | Idempotency | `ConcurrentHashMap` idempotency key registry prevents duplicate orders |
| 20 | Microservice Simulation | ProductService / CartService / OrderService / PaymentService modules |

---

## 🏗 Architecture

```
CLI Menu Runner
      │
      ▼
Service Layer (4 microservice modules)
  ├── ProductService ──────────────┐
  ├── CartService ──── LockManager ├── InMemoryCache (Redis sim)
  ├── OrderService ─────────────── ├── EventBus (Kafka sim)
  └── InventoryService ────────────┘
      │                │
      ▼                ▼
Repository Layer      AuditLogger (Singleton)
  (Spring Data JPA)
      │
      ▼
  H2 In-Memory DB
```

### Design Patterns Used

| Pattern | Location |
|---------|----------|
| **Singleton** | `AuditLogger` (Spring bean) |
| **Factory** | `PaymentProcessorFactory` |
| **Strategy** | `DiscountStrategy` / `CompositeDiscountStrategy` |
| **Observer** | `EventBus` + `EventListener` implementations |
| **State Machine** | `OrderStatus` enum with transition guards |

---

## 🚀 How to Run

### Prerequisites
- Java 17+
- Maven 3.8+

### Build & Run

```bash
cd ecommerce-order-engine

# Build
mvn clean package -DskipTests

# Run
java -jar target/ecommerce-order-engine-1.0.0-SNAPSHOT.jar
```

### Or run directly with Maven

```bash
mvn spring-boot:run
```

---

## 🎮 CLI Menu Guide

```
1.  Add Product              — Register new product (prevents duplicate ID)
2.  View Products            — List all with stock status
3.  Add to Cart              — Reserves stock immediately
4.  Remove from Cart         — Releases stock reservation
5.  View Cart                — Shows items + active reservations
6.  Apply Coupon             — SAVE10 / FLAT200 / EXTRA5
7.  Place Order              — Full 7-step atomic pipeline
8.  Cancel Order             — State-guarded, restores stock
9.  View Orders              — Filter/search by status
10. Low Stock Alert          — Products ≤ 5 units
11. Return Product           — Partial return + refund
12. Simulate Concurrent Users — Multi-thread race condition demo
13. View Audit Logs          — Immutable log viewer
14. Toggle Failure Mode      — Chaos engineering (inject payment failures)
15. Switch User              — Test multi-user isolation
16. Advance Order State      — PAID→SHIPPED→DELIVERED
```

### Discount Rules
| Rule | Condition | Discount |
|------|-----------|----------|
| Bulk Value | Subtotal > ₹1,000 | 10% |
| Bulk Qty | Total items > 3 | 5% |
| Coupon SAVE10 | Any order | 10% |
| Coupon FLAT200 | Order > ₹500 | ₹200 flat |
| Coupon EXTRA5 | Any order | 5% |

---

## 🗄 Database

H2 in-memory by default. Switch to MySQL by:
1. Uncomment MySQL dependency in `pom.xml`
2. Update `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommercedb
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

---

## 📝 Sample Test Flow

```
1. View Products (5 pre-loaded)
2. Switch User → USER_2
3. Add P001 x1 to cart
4. Apply Coupon SAVE10
5. Place Order (UPI)
6. View Orders → see PAID order
7. Simulate Concurrent Users (5 users, P002, qty=1) → see lock in action
8. Enable Failure Mode → Place Order → see FAILED + rollback
9. View Audit Logs → see full immutable trail
```
