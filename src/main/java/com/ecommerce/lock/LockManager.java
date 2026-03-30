package com.ecommerce.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Distributed lock manager using ReentrantLock per resource.
 * Prevents concurrent stock mutations for the same product.
 */
@Component
public class LockManager {

    private static final Logger log = LoggerFactory.getLogger(LockManager.class);
    private static final long LOCK_TIMEOUT_SECONDS = 5;

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private ReentrantLock getLock(String resourceKey) {
        return locks.computeIfAbsent(resourceKey, k -> new ReentrantLock(true)); // fair lock
    }

    public boolean tryLock(String resourceKey) {
        ReentrantLock lock = getLock(resourceKey);
        try {
            boolean acquired = lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (acquired) {
                log.debug("[LOCK] Acquired lock on: {}", resourceKey);
            } else {
                log.warn("[LOCK] Timeout acquiring lock on: {}", resourceKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlock(String resourceKey) {
        ReentrantLock lock = locks.get(resourceKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("[LOCK] Released lock on: {}", resourceKey);
        }
    }

    public void lockAndRun(String resourceKey, Runnable action) {
        if (!tryLock(resourceKey)) {
            throw new IllegalStateException("Could not acquire lock for: " + resourceKey + ". Resource is busy.");
        }
        try {
            action.run();
        } finally {
            unlock(resourceKey);
        }
    }

    public <T> T lockAndGet(String resourceKey, java.util.concurrent.Callable<T> action) {
        if (!tryLock(resourceKey)) {
            throw new IllegalStateException("Could not acquire lock for: " + resourceKey + ". Resource is busy.");
        }
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error during locked operation on: " + resourceKey, e);
        } finally {
            unlock(resourceKey);
        }
    }

    public String productLockKey(String productId) {
        return "PRODUCT:" + productId;
    }

    public String userLockKey(String userId) {
        return "USER:" + userId;
    }
}
