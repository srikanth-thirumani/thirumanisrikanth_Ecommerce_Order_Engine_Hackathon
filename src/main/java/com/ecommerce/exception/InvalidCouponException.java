package com.ecommerce.exception;

public class InvalidCouponException extends RuntimeException {
    public InvalidCouponException(String coupon) {
        super("Invalid or inapplicable coupon: " + coupon);
    }
}
