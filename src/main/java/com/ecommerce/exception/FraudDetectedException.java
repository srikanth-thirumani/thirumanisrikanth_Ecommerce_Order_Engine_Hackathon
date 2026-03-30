package com.ecommerce.exception;

public class FraudDetectedException extends RuntimeException {
    public FraudDetectedException(String userId, String reason) {
        super(String.format("Fraud detected for user %s: %s", userId, reason));
    }
}
