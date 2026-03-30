package com.ecommerce.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Simulates Card payment processing with random success/failure.
 */
public class CardPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(CardPaymentProcessor.class);
    private static final Random random = new Random();
    private final boolean failureInjected;

    public CardPaymentProcessor(boolean failureInjected) {
        this.failureInjected = failureInjected;
    }

    @Override
    public PaymentResult process(String orderId, String userId, BigDecimal amount) {
        log.info("[CARD] Processing payment for order={} user={} amount=₹{}", orderId, userId, amount);
        simulateNetworkDelay();

        if (failureInjected) {
            log.warn("[CARD] FAILURE INJECTED for order {}", orderId);
            return PaymentResult.failure("CARD", "Failure injection triggered");
        }

        // ~15% random failure
        if (random.nextInt(20) < 3) {
            String[] reasons = {"Card declined", "Insufficient funds", "CVV mismatch", "Card expired"};
            String reason = reasons[random.nextInt(reasons.length)];
            log.warn("[CARD] Payment FAILED for order {}: {}", orderId, reason);
            return PaymentResult.failure("CARD", reason);
        }

        String txnId = "CARD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("[CARD] Payment SUCCESS for order={} txn={}", orderId, txnId);
        return PaymentResult.success(txnId, "CARD", amount);
    }

    @Override
    public String getPaymentMethod() {
        return "CARD";
    }

    private void simulateNetworkDelay() {
        try {
            Thread.sleep(150 + random.nextInt(300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
