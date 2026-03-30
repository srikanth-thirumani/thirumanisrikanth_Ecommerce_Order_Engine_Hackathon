package com.ecommerce.service.impl;

import com.ecommerce.audit.AuditLogger;
import com.ecommerce.cache.InMemoryCache;
import com.ecommerce.discount.CompositeDiscountStrategy;
import com.ecommerce.event.DomainEvent;
import com.ecommerce.event.EventBus;
import com.ecommerce.event.EventType;
import com.ecommerce.exception.*;
import com.ecommerce.fraud.FraudDetectionEngine;
import com.ecommerce.idempotency.IdempotencyStore;
import com.ecommerce.lock.LockManager;
import com.ecommerce.model.*;
import com.ecommerce.payment.PaymentProcessorFactory;
import com.ecommerce.payment.PaymentResult;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.InventoryService;
import com.ecommerce.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Microservice simulation: Order Service
 *
 * placeOrder pipeline (all-or-nothing):
 * 1. Idempotency check
 * 2. Fraud detection
 * 3. Cart validation
 * 4. Discount calculation
 * 5. Lock stock
 * 6. Create order (CREATED)
 * 7. -> PENDING_PAYMENT
 * 8. Process payment (Payment Service)
 * 9. On success -> PAID, deduct stock, clear cart, publish events
 * 10. On failure -> FAILED, rollback stock, clear cart
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final PaymentProcessorFactory paymentFactory;
    private final CompositeDiscountStrategy discountStrategy;
    private final FraudDetectionEngine fraudEngine;
    private final IdempotencyStore idempotencyStore;
    private final LockManager lockManager;
    private final InMemoryCache cache;
    private final AuditLogger auditLogger;
    private final EventBus eventBus;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CartItemRepository cartItemRepository,
                            ProductRepository productRepository,
                            InventoryService inventoryService,
                            PaymentProcessorFactory paymentFactory,
                            CompositeDiscountStrategy discountStrategy,
                            FraudDetectionEngine fraudEngine,
                            IdempotencyStore idempotencyStore,
                            LockManager lockManager,
                            InMemoryCache cache,
                            AuditLogger auditLogger,
                            EventBus eventBus) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.paymentFactory = paymentFactory;
        this.discountStrategy = discountStrategy;
        this.fraudEngine = fraudEngine;
        this.idempotencyStore = idempotencyStore;
        this.lockManager = lockManager;
        this.cache = cache;
        this.auditLogger = auditLogger;
        this.eventBus = eventBus;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order placeOrder(String userId, String paymentMethod, String idempotencyKey) {

        // ─────────────────────────────────────────────
        // STEP 1: Idempotency Check
        // ─────────────────────────────────────────────
        if (idempotencyStore.contains(idempotencyKey)) {
            String existingOrderId = idempotencyStore.getOrderId(idempotencyKey).orElse("UNKNOWN");
            log.warn("[ORDER] Duplicate order attempt. idempotencyKey={} existingOrder={}", idempotencyKey, existingOrderId);
            throw new DuplicateOrderException(idempotencyKey);
        }

        // ─────────────────────────────────────────────
        // STEP 2: Fraud Detection
        // ─────────────────────────────────────────────
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty. Cannot place order.");
        }

        BigDecimal subtotal = cartItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        fraudEngine.analyzeOrder(userId, subtotal);

        // ─────────────────────────────────────────────
        // STEP 3: Validate Cart (stock availability)
        // ─────────────────────────────────────────────
        for (CartItem item : cartItems) {
            Product p = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));
            if (p.getAvailableStock() < 0) {
                throw new InsufficientStockException(item.getProductId(), item.getQuantity(), p.getAvailableStock());
            }
        }

        // ─────────────────────────────────────────────
        // STEP 4: Calculate Discount
        // ─────────────────────────────────────────────
        String couponCode = cache.getUserCoupon(userId).orElse(null);
        int totalItems = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal discountAmount = discountStrategy.calculateDiscount(subtotal, totalItems, couponCode);
        BigDecimal totalAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        // ─────────────────────────────────────────────
        // STEP 5: Create Order entity
        // ─────────────────────────────────────────────
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        Order order = Order.builder()
                .orderId(orderId)
                .userId(userId)
                .status(OrderStatus.CREATED)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .couponCode(couponCode)
                .paymentMethod(paymentMethod)
                .idempotencyKey(idempotencyKey)
                .build();

        // Build order items
        List<OrderItem> orderItems = cartItems.stream()
                .map(ci -> OrderItem.builder()
                        .order(order)
                        .productId(ci.getProductId())
                        .productName(ci.getProductName())
                        .quantity(ci.getQuantity())
                        .unitPrice(ci.getUnitPrice())
                        .returnedQuantity(0)
                        .build())
                .collect(Collectors.toList());
        order.setItems(orderItems);

        // Register idempotency key BEFORE saving (prevents race condition)
        idempotencyStore.register(idempotencyKey, orderId);

        Order savedOrder = orderRepository.save(order);

        auditLogger.log(userId, "ORDER_CREATED", "ORDER", orderId,
                String.format("subtotal=%.2f discount=%.2f total=%.2f method=%s coupon=%s",
                        subtotal.doubleValue(), discountAmount.doubleValue(), totalAmount.doubleValue(),
                        paymentMethod, couponCode));

        eventBus.publishSync(DomainEvent.of(EventType.ORDER_CREATED, userId, orderId, "ORDER",
                Map.of("total", totalAmount, "method", paymentMethod)));

        // ─────────────────────────────────────────────
        // STEP 6: Transition to PENDING_PAYMENT
        // ─────────────────────────────────────────────
        savedOrder.transitionTo(OrderStatus.PENDING_PAYMENT);
        savedOrder = orderRepository.save(savedOrder);
        log.info("[ORDER] {} -> PENDING_PAYMENT", orderId);

        // ─────────────────────────────────────────────
        // STEP 7: Process Payment (Microservice: Payment Service)
        // ─────────────────────────────────────────────
        PaymentResult paymentResult = paymentFactory.create(paymentMethod)
                .process(orderId, userId, totalAmount);

        if (paymentResult.isSuccess()) {
            // ─── Payment SUCCESS ───
            log.info("[ORDER] Payment SUCCESS for order {} txn={}", orderId, paymentResult.getTransactionId());

            // Deduct stock for each item
            for (CartItem item : cartItems) {
                inventoryService.deductReservedStock(userId, item.getProductId(), item.getQuantity());
            }

            savedOrder.transitionTo(OrderStatus.PAID);
            savedOrder = orderRepository.save(savedOrder);

            // Clear cart
            cartItemRepository.deleteByUserId(userId);
            cache.clearUserCoupon(userId);

            auditLogger.log(userId, "PAYMENT_SUCCESS", "ORDER", orderId,
                    String.format("txn=%s amount=%.2f method=%s",
                            paymentResult.getTransactionId(), totalAmount.doubleValue(), paymentMethod));

            eventBus.publishSync(DomainEvent.of(EventType.PAYMENT_SUCCESS, userId, orderId, "ORDER",
                    Map.of("txn", paymentResult.getTransactionId(), "amount", totalAmount)));

            log.info("[ORDER] ✓ Order {} completed. Status: PAID. Total: ₹{}", orderId, totalAmount);
            return savedOrder;

        } else {
            // ─── Payment FAILED → Rollback ───
            log.warn("[ORDER] Payment FAILED for order {}: {}", orderId, paymentResult.getFailureReason());

            // Rollback: release all reserved stock
            try {
                inventoryService.releaseAllUserStock(userId);
            } catch (Exception ex) {
                log.error("[ORDER] Error releasing stock during rollback: {}", ex.getMessage());
            }

            savedOrder.transitionTo(OrderStatus.FAILED);
            savedOrder.setFailureReason(paymentResult.getFailureReason());
            savedOrder = orderRepository.save(savedOrder);

            // Clear cart even on failure (user must restart)
            cartItemRepository.deleteByUserId(userId);
            cache.clearUserCoupon(userId);

            auditLogger.log(userId, "PAYMENT_FAILED", "ORDER", orderId,
                    "reason=" + paymentResult.getFailureReason());

            eventBus.publishSync(DomainEvent.of(EventType.PAYMENT_FAILED, userId, orderId, "ORDER",
                    Map.of("reason", paymentResult.getFailureReason())));

            throw new PaymentFailedException(paymentResult.getFailureReason());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(String userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order cancelOrder(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new OrderCancellationException("Order does not belong to user: " + userId);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderCancellationException("Order is already cancelled: " + orderId);
        }

        if (!order.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new OrderCancellationException(
                    String.format("Cannot cancel order in state: %s (orderId=%s)", order.getStatus(), orderId));
        }

        // Restore stock for paid/shipped orders
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.SHIPPED) {
            for (OrderItem item : order.getItems()) {
                lockManager.lockAndRun(lockManager.productLockKey(item.getProductId()), () -> {
                    productRepository.findByIdWithLock(item.getProductId()).ifPresent(p -> {
                        p.setAvailableStock(p.getAvailableStock() + item.getQuantity());
                        p.setTotalStock(p.getTotalStock() + item.getQuantity());
                        productRepository.save(p);
                        cache.evictProduct(p.getProductId());
                    });
                });
            }
        }

        order.transitionTo(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        auditLogger.log(userId, "ORDER_CANCELLED", "ORDER", orderId, "status=CANCELLED");
        eventBus.publish(DomainEvent.of(EventType.ORDER_CANCELLED, userId, orderId, "ORDER",
                Map.of("previousStatus", order.getStatus().name())));

        log.info("[ORDER] ✗ Order {} cancelled by user {}", orderId, userId);
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order returnItems(String orderId, String productId, int returnQuantity) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.DELIVERED
                && order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Returns only allowed for PAID/SHIPPED/DELIVERED orders");
        }

        OrderItem targetItem = order.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not in order: " + productId));

        if (returnQuantity <= 0 || returnQuantity > targetItem.getReturnableQuantity()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid return quantity %d. Returnable: %d", returnQuantity, targetItem.getReturnableQuantity()));
        }

        // Restore stock
        lockManager.lockAndRun(lockManager.productLockKey(productId), () ->
                productRepository.findByIdWithLock(productId).ifPresent(p -> {
                    p.setAvailableStock(p.getAvailableStock() + returnQuantity);
                    p.setTotalStock(p.getTotalStock() + returnQuantity);
                    productRepository.save(p);
                    cache.evictProduct(productId);
                })
        );

        // Adjust order total
        BigDecimal refundAmount = targetItem.getUnitPrice()
                .multiply(BigDecimal.valueOf(returnQuantity));
        order.setTotalAmount(order.getTotalAmount().subtract(refundAmount).max(BigDecimal.ZERO));

        targetItem.setReturnedQuantity(targetItem.getReturnedQuantity() + returnQuantity);
        order.setStatus(OrderStatus.PARTIALLY_RETURNED);
        Order saved = orderRepository.save(order);

        auditLogger.log(order.getUserId(), "ORDER_RETURN", "ORDER", orderId,
                String.format("product=%s qty=%d refund=%.2f", productId, returnQuantity, refundAmount.doubleValue()));

        eventBus.publish(DomainEvent.of(EventType.ORDER_RETURNED, order.getUserId(), orderId, "ORDER",
                Map.of("product", productId, "qty", returnQuantity, "refund", refundAmount)));

        log.info("[ORDER] Partial return: order={} product={} qty={} refund=₹{}", orderId, productId, returnQuantity, refundAmount);
        return saved;
    }

    @Override
    @Transactional
    public void advanceOrderState(String orderId, OrderStatus targetStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.transitionTo(targetStatus);
        orderRepository.save(order);
        auditLogger.logSystem("ORDER_STATE_ADVANCED", "ORDER", orderId,
                "newStatus=" + targetStatus);
        log.info("[ORDER] {} advanced to {}", orderId, targetStatus);
    }
}
