package com.ecommerce.service;

import com.ecommerce.model.CartItem;

import java.util.List;

/**
 * Cart Service — Microservice simulation: Cart Service module
 */
public interface CartService {
    CartItem addToCart(String userId, String productId, int quantity);
    void removeFromCart(String userId, String productId);
    void updateCartItemQuantity(String userId, String productId, int newQuantity);
    List<CartItem> getCart(String userId);
    void clearCart(String userId);
    void applyCoupon(String userId, String couponCode);
    String getAppliedCoupon(String userId);
}
