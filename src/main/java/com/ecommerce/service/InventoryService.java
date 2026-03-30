package com.ecommerce.service;

import com.ecommerce.model.StockReservation;

import java.util.List;

/**
 * Inventory Service — Microservice simulation: handles reservations and expiry
 */
public interface InventoryService {
    void reserveStock(String userId, String productId, int quantity);
    void releaseStock(String userId, String productId);
    void releaseAllUserStock(String userId);
    void deductReservedStock(String userId, String productId, int quantity);
    List<StockReservation> getActiveReservations(String userId);
    void processExpiredReservations();
}
