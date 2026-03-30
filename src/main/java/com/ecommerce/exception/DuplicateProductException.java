package com.ecommerce.exception;

public class DuplicateProductException extends RuntimeException {
    public DuplicateProductException(String productId) {
        super("Product with ID already exists: " + productId);
    }
}
