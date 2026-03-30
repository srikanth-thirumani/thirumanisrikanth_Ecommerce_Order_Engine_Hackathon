package com.ecommerce.cache;

import com.ecommerce.model.Product;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache simulating Redis.
 * Thread-safe via ConcurrentHashMap.
 */
@Component
public class InMemoryCache {

    private final Map<String, Product> productCache = new ConcurrentHashMap<>();
    private final Map<String, String> userCoupons = new ConcurrentHashMap<>();
    private final Map<String, String> flaggedUsers = new ConcurrentHashMap<>();

    // --- Product Cache ---
    public void putProduct(Product product) {
        productCache.put(product.getProductId(), product);
    }

    public Optional<Product> getProduct(String productId) {
        return Optional.ofNullable(productCache.get(productId));
    }

    public void evictProduct(String productId) {
        productCache.remove(productId);
    }

    public Collection<Product> getAllCachedProducts() {
        return productCache.values();
    }

    public void clearProductCache() {
        productCache.clear();
    }

    // --- User Coupon Cache ---
    public void setUserCoupon(String userId, String couponCode) {
        userCoupons.put(userId, couponCode);
    }

    public Optional<String> getUserCoupon(String userId) {
        return Optional.ofNullable(userCoupons.get(userId));
    }

    public void clearUserCoupon(String userId) {
        userCoupons.remove(userId);
    }

    // --- Fraud Flag Cache ---
    public void flagUser(String userId, String reason) {
        flaggedUsers.put(userId, reason);
    }

    public boolean isUserFlagged(String userId) {
        return flaggedUsers.containsKey(userId);
    }

    public String getFlagReason(String userId) {
        return flaggedUsers.get(userId);
    }

    public Map<String, String> getAllFlaggedUsers() {
        return Map.copyOf(flaggedUsers);
    }
}
