package com.ecommerce.payment;

import java.math.BigDecimal;

/**
 * Factory pattern: PaymentProcessor interface.
 */
public interface PaymentProcessor {
    PaymentResult process(String orderId, String userId, BigDecimal amount);
    String getPaymentMethod();
}
