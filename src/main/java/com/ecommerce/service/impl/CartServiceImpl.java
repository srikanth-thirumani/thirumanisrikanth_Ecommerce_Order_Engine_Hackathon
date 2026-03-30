package com.ecommerce.service.impl;

import com.ecommerce.audit.AuditLogger;
import com.ecommerce.cache.InMemoryCache;
import com.ecommerce.discount.CompositeDiscountStrategy;
import com.ecommerce.event.DomainEvent;
import com.ecommerce.event.EventBus;
import com.ecommerce.event.EventType;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.InvalidCouponException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.lock.LockManager;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.CartService;
import com.ecommerce.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Microservice simulation: Cart Service
 * Each user has an independent cart.
 * Add-to-cart triggers stock reservation.
 * Remove-from-cart releases reservation.
 */
@Service
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final InMemoryCache cache;
    private final LockManager lockManager;
    private final AuditLogger auditLogger;
    private final EventBus eventBus;

    public CartServiceImpl(CartItemRepository cartItemRepository,
                           ProductRepository productRepository,
                           InventoryService inventoryService,
                           InMemoryCache cache,
                           LockManager lockManager,
                           AuditLogger auditLogger,
                           EventBus eventBus) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.cache = cache;
        this.lockManager = lockManager;
        this.auditLogger = auditLogger;
        this.eventBus = eventBus;
    }

    @Override
    @Transactional
    public CartItem addToCart(String userId, String productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");

        return lockManager.lockAndGet(lockManager.productLockKey(productId), () -> {
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));

            if (product.getAvailableStock() == 0) {
                throw new InsufficientStockException(productId, quantity, 0);
            }

            Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(userId, productId);

            if (existing.isPresent()) {
                // Update existing cart item
                CartItem item = existing.get();
                int additionalQty = quantity;
                if (product.getAvailableStock() < additionalQty) {
                    throw new InsufficientStockException(productId, additionalQty, product.getAvailableStock());
                }
                item.setQuantity(item.getQuantity() + additionalQty);
                inventoryService.reserveStock(userId, productId, additionalQty);
                CartItem saved = cartItemRepository.save(item);
                cache.putProduct(productRepository.findById(productId).orElse(product));
                auditLogger.log(userId, "CART_ITEM_UPDATED", "CART", productId,
                        String.format("qty=%d total=%d", additionalQty, saved.getQuantity()));
                return saved;
            } else {
                // New cart item
                if (product.getAvailableStock() < quantity) {
                    throw new InsufficientStockException(productId, quantity, product.getAvailableStock());
                }
                CartItem item = CartItem.builder()
                        .userId(userId)
                        .productId(productId)
                        .productName(product.getName())
                        .quantity(quantity)
                        .unitPrice(product.getPrice())
                        .build();
                inventoryService.reserveStock(userId, productId, quantity);
                CartItem saved = cartItemRepository.save(item);
                cache.putProduct(productRepository.findById(productId).orElse(product));

                auditLogger.log(userId, "CART_ITEM_ADDED", "CART", productId,
                        String.format("qty=%d price=%.2f", quantity, product.getPrice().doubleValue()));

                eventBus.publish(DomainEvent.of(EventType.CART_UPDATED, userId, productId, "CART",
                        Map.of("action", "ADD", "qty", quantity)));

                log.info("[CART] {} added {}x {} to cart", userId, quantity, productId);
                return saved;
            }
        });
    }

    @Override
    @Transactional
    public void removeFromCart(String userId, String productId) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new IllegalStateException("Item not in cart: " + productId));

        inventoryService.releaseStock(userId, productId);
        cartItemRepository.deleteByUserIdAndProductId(userId, productId);

        auditLogger.log(userId, "CART_ITEM_REMOVED", "CART", productId,
                String.format("qty=%d released", item.getQuantity()));

        log.info("[CART] {} removed {} from cart", userId, productId);
    }

    @Override
    @Transactional
    public void updateCartItemQuantity(String userId, String productId, int newQuantity) {
        if (newQuantity <= 0) {
            removeFromCart(userId, productId);
            return;
        }

        lockManager.lockAndRun(lockManager.productLockKey(productId), () -> {
            CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                    .orElseThrow(() -> new IllegalStateException("Item not in cart: " + productId));

            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));

            int oldQty = item.getQuantity();
            int delta = newQuantity - oldQty;

            if (delta > 0) {
                // Need more stock
                if (product.getAvailableStock() < delta) {
                    throw new InsufficientStockException(productId, delta, product.getAvailableStock());
                }
                inventoryService.reserveStock(userId, productId, delta);
            } else if (delta < 0) {
                // Release some stock
                int releaseQty = Math.abs(delta);
                inventoryService.releaseStock(userId, productId);
                // Re-reserve correct amount
                inventoryService.reserveStock(userId, productId, newQuantity);
            }

            item.setQuantity(newQuantity);
            cartItemRepository.save(item);

            auditLogger.log(userId, "CART_QTY_UPDATED", "CART", productId,
                    String.format("oldQty=%d newQty=%d", oldQty, newQuantity));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getCart(String userId) {
        return cartItemRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void clearCart(String userId) {
        cartItemRepository.deleteByUserId(userId);
        cache.clearUserCoupon(userId);
        log.info("[CART] Cleared cart for user {}", userId);
    }

    @Override
    public void applyCoupon(String userId, String couponCode) {
        if (!CompositeDiscountStrategy.isValidCoupon(couponCode)) {
            throw new InvalidCouponException(couponCode);
        }
        cache.setUserCoupon(userId, couponCode.toUpperCase());
        auditLogger.log(userId, "COUPON_APPLIED", "CART", userId, "coupon=" + couponCode.toUpperCase());
        log.info("[CART] User {} applied coupon: {}", userId, couponCode.toUpperCase());
    }

    @Override
    public String getAppliedCoupon(String userId) {
        return cache.getUserCoupon(userId).orElse(null);
    }
}
