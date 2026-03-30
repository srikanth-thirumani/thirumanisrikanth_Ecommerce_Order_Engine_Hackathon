package com.ecommerce.service.impl;

import com.ecommerce.audit.AuditLogger;
import com.ecommerce.event.DomainEvent;
import com.ecommerce.event.EventBus;
import com.ecommerce.event.EventType;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.lock.LockManager;
import com.ecommerce.model.Product;
import com.ecommerce.model.StockReservation;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.StockReservationRepository;
import com.ecommerce.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Microservice simulation: Inventory / Stock Reservation Service
 * Handles real-time reservation, release, expiry, deduction.
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;
    private final LockManager lockManager;
    private final AuditLogger auditLogger;
    private final EventBus eventBus;

    @Value("${app.reservation.expiry.seconds:120}")
    private int expirySeconds;

    public InventoryServiceImpl(ProductRepository productRepository,
                                StockReservationRepository reservationRepository,
                                LockManager lockManager,
                                AuditLogger auditLogger,
                                EventBus eventBus) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.lockManager = lockManager;
        this.auditLogger = auditLogger;
        this.eventBus = eventBus;
    }

    @Override
    @Transactional
    public void reserveStock(String userId, String productId, int quantity) {
        lockManager.lockAndRun(lockManager.productLockKey(productId), () -> {
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));

            product.reserveStock(quantity); // throws if insufficient
            productRepository.save(product);

            // Check if reservation already exists for this user+product
            Optional<StockReservation> existing =
                    reservationRepository.findByUserIdAndProductIdAndReleasedFalse(userId, productId);

            if (existing.isPresent()) {
                StockReservation res = existing.get();
                res.setQuantity(res.getQuantity() + quantity);
                res.setExpiresAt(LocalDateTime.now().plusSeconds(expirySeconds));
                reservationRepository.save(res);
            } else {
                StockReservation reservation = StockReservation.builder()
                        .userId(userId)
                        .productId(productId)
                        .quantity(quantity)
                        .reservedAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusSeconds(expirySeconds))
                        .released(false)
                        .build();
                reservationRepository.save(reservation);
            }

            log.info("[INVENTORY] Reserved {}x {} for user {}", quantity, productId, userId);
            auditLogger.log(userId, "STOCK_RESERVED", "PRODUCT", productId,
                    String.format("qty=%d expiresIn=%ds", quantity, expirySeconds));

            eventBus.publish(DomainEvent.of(EventType.INVENTORY_RESERVED, userId, productId, "PRODUCT",
                    Map.of("qty", quantity)));
        });
    }

    @Override
    @Transactional
    public void releaseStock(String userId, String productId) {
        lockManager.lockAndRun(lockManager.productLockKey(productId), () -> {
            Optional<StockReservation> optRes =
                    reservationRepository.findByUserIdAndProductIdAndReleasedFalse(userId, productId);

            if (optRes.isEmpty()) {
                log.warn("[INVENTORY] No active reservation to release for user={} product={}", userId, productId);
                return;
            }

            StockReservation reservation = optRes.get();
            int qty = reservation.getQuantity();

            reservation.setReleased(true);
            reservation.setReleasedAt(LocalDateTime.now());
            reservationRepository.save(reservation);

            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
            product.releaseStock(qty);
            productRepository.save(product);

            log.info("[INVENTORY] Released {}x {} for user {}", qty, productId, userId);
            auditLogger.log(userId, "STOCK_RELEASED", "PRODUCT", productId, "qty=" + qty);

            eventBus.publish(DomainEvent.of(EventType.INVENTORY_RELEASED, userId, productId, "PRODUCT",
                    Map.of("qty", qty)));
        });
    }

    @Override
    @Transactional
    public void releaseAllUserStock(String userId) {
        List<StockReservation> reservations = reservationRepository.findByUserIdAndReleasedFalse(userId);
        for (StockReservation res : reservations) {
            try {
                releaseStock(userId, res.getProductId());
            } catch (Exception e) {
                log.error("[INVENTORY] Error releasing stock for {}:{} - {}", userId, res.getProductId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void deductReservedStock(String userId, String productId, int quantity) {
        lockManager.lockAndRun(lockManager.productLockKey(productId), () -> {
            StockReservation reservation = reservationRepository
                    .findByUserIdAndProductIdAndReleasedFalse(userId, productId)
                    .orElseThrow(() -> new IllegalStateException(
                            "No active reservation for user=" + userId + " product=" + productId));

            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));

            product.deductReservedStock(quantity);
            productRepository.save(product);

            reservation.setReleased(true);
            reservation.setReleasedAt(LocalDateTime.now());
            reservationRepository.save(reservation);

            log.info("[INVENTORY] Deducted {}x {} for order confirmation (user={})", quantity, productId, userId);
            auditLogger.log(userId, "STOCK_DEDUCTED", "PRODUCT", productId, "qty=" + quantity);

            eventBus.publish(DomainEvent.of(EventType.INVENTORY_UPDATED, userId, productId, "PRODUCT",
                    Map.of("action", "DEDUCT", "qty", quantity)));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockReservation> getActiveReservations(String userId) {
        return reservationRepository.findByUserIdAndReleasedFalse(userId);
    }

    /**
     * Feature 15: Scheduled expiry check every 30 seconds.
     */
    @Override
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processExpiredReservations() {
        List<StockReservation> expired = reservationRepository.findExpiredReservations(LocalDateTime.now());
        if (!expired.isEmpty()) {
            log.info("[INVENTORY] Processing {} expired reservations...", expired.size());
        }
        for (StockReservation res : expired) {
            try {
                res.setReleased(true);
                res.setReleasedAt(LocalDateTime.now());
                reservationRepository.save(res);

                Product product = productRepository.findByIdWithLock(res.getProductId()).orElse(null);
                if (product != null) {
                    product.releaseStock(res.getQuantity());
                    productRepository.save(product);
                    log.info("[INVENTORY] Auto-released expired reservation: user={} product={} qty={}",
                            res.getUserId(), res.getProductId(), res.getQuantity());
                    auditLogger.logSystem("RESERVATION_EXPIRED", "PRODUCT", res.getProductId(),
                            String.format("user=%s qty=%d", res.getUserId(), res.getQuantity()));

                    eventBus.publish(DomainEvent.of(EventType.RESERVATION_EXPIRED,
                            res.getUserId(), res.getProductId(), "PRODUCT",
                            Map.of("qty", res.getQuantity())));
                }
            } catch (Exception e) {
                log.error("[INVENTORY] Failed to release expired reservation {}: {}", res.getId(), e.getMessage());
            }
        }
    }
}
