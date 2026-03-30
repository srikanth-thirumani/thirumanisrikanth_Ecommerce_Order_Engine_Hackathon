package com.ecommerce.fraud;

import com.ecommerce.cache.InMemoryCache;
import com.ecommerce.exception.FraudDetectedException;
import com.ecommerce.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fraud Detection Engine.
 * Rules:
 * 1. 3 orders in 1 minute → flag user
 * 2. Order value > threshold → suspicious
 */
@Component
public class FraudDetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionEngine.class);

    private final OrderRepository orderRepository;
    private final InMemoryCache cache;

    @Value("${app.fraud.order.count.limit:3}")
    private int orderCountLimit;

    @Value("${app.fraud.order.window.minutes:1}")
    private int orderWindowMinutes;

    @Value("${app.fraud.high.value.threshold:50000}")
    private double highValueThreshold;

    public FraudDetectionEngine(OrderRepository orderRepository, InMemoryCache cache) {
        this.orderRepository = orderRepository;
        this.cache = cache;
    }

    public void analyzeOrder(String userId, BigDecimal orderAmount) {
        if (cache.isUserFlagged(userId)) {
            String reason = cache.getFlagReason(userId);
            log.warn("[FRAUD] User {} is already flagged: {}", userId, reason);
            throw new FraudDetectedException(userId, "Previously flagged - " + reason);
        }

        // Rule 1: Rapid successive orders
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(orderWindowMinutes);
        long recentOrders = orderRepository.countByUserIdAndCreatedAtAfter(userId, windowStart);

        if (recentOrders >= orderCountLimit) {
            String reason = String.format("Placed %d orders within %d minute(s)", recentOrders, orderWindowMinutes);
            log.warn("[FRAUD] Suspicious activity for user {}: {}", userId, reason);
            cache.flagUser(userId, reason);
            throw new FraudDetectedException(userId, reason);
        }

        // Rule 2: High value order
        if (orderAmount.compareTo(BigDecimal.valueOf(highValueThreshold)) > 0) {
            String reason = String.format("High value order: ₹%.2f (threshold: ₹%.2f)", orderAmount.doubleValue(), highValueThreshold);
            log.warn("[FRAUD] High-value order detected for user {}: {}", userId, reason);
            cache.flagUser(userId, reason);
            // We warn but don't block high-value — just flag for review
        }

        log.debug("[FRAUD] User {} passed fraud checks", userId);
    }

    public boolean isUserFlagged(String userId) {
        return cache.isUserFlagged(userId);
    }
}
