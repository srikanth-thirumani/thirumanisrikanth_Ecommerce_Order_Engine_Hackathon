package com.ecommerce.discount;

import java.math.BigDecimal;
import java.util.List;

/**
 * Strategy pattern: Discount calculation context.
 */
public interface DiscountStrategy {
    BigDecimal calculateDiscount(BigDecimal subtotal, int totalItems, String couponCode);
    String getDescription();
}
