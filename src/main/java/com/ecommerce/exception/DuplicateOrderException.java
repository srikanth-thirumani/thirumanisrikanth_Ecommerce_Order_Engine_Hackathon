package com.ecommerce.exception;

public class DuplicateOrderException extends RuntimeException {
    public DuplicateOrderException(String idempotencyKey) {
        super("Duplicate order detected. Idempotency key already used: " + idempotencyKey);
    }
}
