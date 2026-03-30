package com.ecommerce.service.impl;

import com.ecommerce.audit.AuditLogger;
import com.ecommerce.model.Product;
import com.ecommerce.service.CartService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Feature 4: Concurrency Simulation
 * Submits multiple user threads simultaneously trying to buy
 * the same limited-stock product. ReentrantLock ensures only
 * valid orders succeed; overselling is prevented.
 */
@Service
public class ConcurrencySimulatorService {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencySimulatorService.class);

    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final AuditLogger auditLogger;

    public ConcurrencySimulatorService(ProductService productService,
                                       CartService cartService,
                                       OrderService orderService,
                                       AuditLogger auditLogger) {
        this.productService = productService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.auditLogger = auditLogger;
    }

    public SimulationResult simulate(int userCount, String productId, int qtyPerUser, String paymentMethod) {
        log.info("[CONCURRENCY] Starting simulation: {} users trying to buy {}x {}",
                userCount, qtyPerUser, productId);

        Product product = productService.getProduct(productId);
        log.info("[CONCURRENCY] Product: {} | Available stock: {}", product.getName(), product.getAvailableStock());

        ExecutorService executor = Executors.newFixedThreadPool(userCount,
                r -> new Thread(r, "SimUser-" + UUID.randomUUID().toString().substring(0, 4)));

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(userCount);

        for (int i = 1; i <= userCount; i++) {
            final String userId = "SIM_USER_" + i;
            executor.submit(() -> {
                try {
                    startGate.await(); // All threads start simultaneously
                    String idempotencyKey = "SIM-" + userId + "-" + productId + "-" + System.nanoTime();

                    cartService.addToCart(userId, productId, qtyPerUser);
                    orderService.placeOrder(userId, paymentMethod, idempotencyKey);

                    successCount.incrementAndGet();
                    String msg = String.format("✓ %s: ORDER SUCCESS", userId);
                    results.add(msg);
                    log.info("[CONCURRENCY] {}", msg);

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    String msg = String.format("✗ %s: FAILED - %s", userId, e.getMessage());
                    results.add(msg);
                    log.warn("[CONCURRENCY] {}", msg);

                    // Release any reserved stock on failure
                    try {
                        List<com.ecommerce.model.CartItem> cart = cartService.getCart(userId);
                        if (!cart.isEmpty()) {
                            cartService.clearCart(userId);
                        }
                    } catch (Exception ex) {
                        // best effort cleanup
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown(); // Release all threads at once

        try {
            doneLatch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

        // Verify final stock
        Product finalProduct = productService.getProduct(productId);
        log.info("[CONCURRENCY] Simulation complete. Success={} Failure={} FinalStock={}",
                successCount.get(), failureCount.get(), finalProduct.getAvailableStock());

        auditLogger.logSystem("CONCURRENCY_SIMULATION", "PRODUCT", productId,
                String.format("users=%d success=%d failure=%d finalStock=%d",
                        userCount, successCount.get(), failureCount.get(), finalProduct.getAvailableStock()));

        return new SimulationResult(userCount, successCount.get(), failureCount.get(),
                finalProduct.getAvailableStock(), results);
    }

    public record SimulationResult(
            int totalUsers,
            int successCount,
            int failureCount,
            int finalStock,
            List<String> details
    ) {}
}
