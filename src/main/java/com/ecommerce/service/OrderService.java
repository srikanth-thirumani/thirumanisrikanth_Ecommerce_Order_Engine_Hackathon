package com.ecommerce.service;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;

import java.util.List;

/**
 * Order Service — Microservice simulation: Order Service module
 */
public interface OrderService {
    Order placeOrder(String userId, String paymentMethod, String idempotencyKey);
    Order getOrder(String orderId);
    List<Order> getAllOrders();
    List<Order> getOrdersByUser(String userId);
    List<Order> getOrdersByStatus(OrderStatus status);
    Order cancelOrder(String orderId, String userId);
    Order returnItems(String orderId, String productId, int quantity);
    void advanceOrderState(String orderId, OrderStatus targetStatus);
}
