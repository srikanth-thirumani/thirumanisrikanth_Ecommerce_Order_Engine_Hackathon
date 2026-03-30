package com.ecommerce.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Simulates COD (Cash on Delivery) — always succeeds unless manually failed.
 */
public class CodPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(CodPaymentProcessor.class);
    private static final Random random = new Random();
    private final boolean failureInjected;

    public CodPaymentProcessor(boolean failureInjected) {
        this.failureInjected = failureInjected;
    }

    @Override
    public PaymentResult process(String orderId, String userId, BigDecimal amount) {
        log.info("[COD] Processing COD order={} user={} amount=₹{}", orderId, userId, amount);

        if (failureInjected) {
            return PaymentResult.failure("COD", "Failure injection triggered");
        }

        String txnId = "COD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[COD] COD Confirmed for order={} txn={}", orderId, txnId);
        return PaymentResult.success(txnId, "COD", amount);
    }

    @Override
    public String getPaymentMethod() {
        return "COD";
    }
}
