package com.ecommerce.idempotency;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory idempotency store.
 * Prevents duplicate order placement on multiple rapid clicks.
 * Maps idempotency-key → orderId.
 */
@Component
public class IdempotencyStore {

    // Maps idempotencyKey → orderId
    private final Map<String, String> store = new ConcurrentHashMap<>();

    public boolean contains(String key) {
        return store.containsKey(key);
    }

    public void register(String key, String orderId) {
        store.putIfAbsent(key, orderId);
    }

    public Optional<String> getOrderId(String key) {
        return Optional.ofNullable(store.get(key));
    }

    public void invalidate(String key) {
        store.remove(key);
    }
}
