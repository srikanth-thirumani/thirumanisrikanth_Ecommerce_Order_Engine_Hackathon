package com.ecommerce.discount;

import com.ecommerce.exception.InvalidCouponException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Composite discount strategy:
 * - Subtotal > 1000 → 10% off
 * - Total items > 3 → extra 5% off
 * - Coupon: SAVE10 → 10%, FLAT200 → ₹200 flat
 * Discounts stack but coupons are validated against invalid combinations.
 */
@Component
public class CompositeDiscountStrategy implements DiscountStrategy {

    private static final Map<String, String> VALID_COUPONS = new HashMap<>();

    static {
        VALID_COUPONS.put("SAVE10", "PERCENTAGE_10");
        VALID_COUPONS.put("FLAT200", "FLAT_200");
        VALID_COUPONS.put("EXTRA5", "PERCENTAGE_5");
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, int totalItems, String couponCode) {
        BigDecimal totalDiscount = BigDecimal.ZERO;

        // Rule 1: subtotal > 1000 → 10% discount
        if (subtotal.compareTo(BigDecimal.valueOf(1000)) > 0) {
            BigDecimal discount = subtotal.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
            totalDiscount = totalDiscount.add(discount);
        }

        // Rule 2: qty > 3 → extra 5%
        if (totalItems > 3) {
            BigDecimal discount = subtotal.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
            totalDiscount = totalDiscount.add(discount);
        }

        // Rule 3: Coupon application
        if (couponCode != null && !couponCode.isBlank()) {
            String couponType = VALID_COUPONS.get(couponCode.toUpperCase());
            if (couponType == null) {
                throw new InvalidCouponException(couponCode);
            }
            // Avoid invalid combination: FLAT200 requires subtotal > 500
            if ("FLAT_200".equals(couponType) && subtotal.compareTo(BigDecimal.valueOf(500)) < 0) {
                throw new InvalidCouponException("FLAT200 requires order value > ₹500");
            }
            // Avoid stacking SAVE10 + EXTRA5 (too much discount)
            if ("PERCENTAGE_10".equals(couponType) || "PERCENTAGE_5".equals(couponType)) {
                if ("PERCENTAGE_10".equals(couponType) && totalItems > 3) {
                    // Allow but cap total at 20%
                    BigDecimal extraDiscount = subtotal.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal maxAllowed = subtotal.multiply(BigDecimal.valueOf(0.20)).setScale(2, RoundingMode.HALF_UP);
                    totalDiscount = totalDiscount.add(extraDiscount).min(maxAllowed);
                } else {
                    BigDecimal pct = "PERCENTAGE_10".equals(couponType) ? BigDecimal.valueOf(0.10) : BigDecimal.valueOf(0.05);
                    BigDecimal extraDiscount = subtotal.multiply(pct).setScale(2, RoundingMode.HALF_UP);
                    totalDiscount = totalDiscount.add(extraDiscount);
                }
            } else if ("FLAT_200".equals(couponType)) {
                totalDiscount = totalDiscount.add(BigDecimal.valueOf(200));
            }
        }

        // Discount cannot exceed subtotal
        if (totalDiscount.compareTo(subtotal) > 0) {
            totalDiscount = subtotal;
        }

        return totalDiscount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getDescription() {
        return "Composite: 10% for >1000, 5% for qty>3, Coupons: SAVE10/FLAT200/EXTRA5";
    }

    public static boolean isValidCoupon(String code) {
        return code != null && VALID_COUPONS.containsKey(code.toUpperCase());
    }
}
