package com.ecommerce.payment;

import org.springframework.stereotype.Component;

/**
 * Factory pattern: Creates the appropriate PaymentProcessor.
 * failureInjected flag enables failure injection mode (Feature 18).
 */
@Component
public class PaymentProcessorFactory {

    private volatile boolean failureInjected = false;

    public PaymentProcessor create(String method) {
        return switch (method.toUpperCase()) {
            case "UPI"  -> new UpiPaymentProcessor(failureInjected);
            case "CARD" -> new CardPaymentProcessor(failureInjected);
            case "COD"  -> new CodPaymentProcessor(failureInjected);
            default     -> throw new IllegalArgumentException("Unknown payment method: " + method + ". Valid: UPI, CARD, COD");
        };
    }

    public void setFailureInjected(boolean failureInjected) {
        this.failureInjected = failureInjected;
    }

    public boolean isFailureInjected() {
        return failureInjected;
    }
}
