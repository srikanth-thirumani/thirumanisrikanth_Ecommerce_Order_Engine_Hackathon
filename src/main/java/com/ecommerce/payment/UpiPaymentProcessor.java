package com.ecommerce.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Simulates UPI payment processing with random success/failure.
 */
public class UpiPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(UpiPaymentProcessor.class);
    private static final Random random = new Random();
    private final boolean failureInjected;

    public UpiPaymentProcessor(boolean failureInjected) {
        this.failureInjected = failureInjected;
    }

    @Override
    public PaymentResult process(String orderId, String userId, BigDecimal amount) {
        log.info("[UPI] Processing payment for order={} user={} amount=₹{}", orderId, userId, amount);
        simulateNetworkDelay();

        if (failureInjected) {
            log.warn("[UPI] FAILURE INJECTED for order {}", orderId);
            return PaymentResult.failure("UPI", "Failure injection triggered");
        }

        // ~20% random failure rate
        if (random.nextInt(10) < 2) {
            String reason = random.nextBoolean() ? "Bank declined transaction" : "UPI timeout";
            log.warn("[UPI] Payment FAILED for order {}: {}", orderId, reason);
            return PaymentResult.failure("UPI", reason);
        }

        String txnId = "UPI-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("[UPI] Payment SUCCESS for order={} txn={}", orderId, txnId);
        return PaymentResult.success(txnId, "UPI", amount);
    }

    @Override
    public String getPaymentMethod() {
        return "UPI";
    }

    private void simulateNetworkDelay() {
        try {
            Thread.sleep(100 + random.nextInt(200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
