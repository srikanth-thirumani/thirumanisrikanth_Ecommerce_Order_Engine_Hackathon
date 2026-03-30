package com.ecommerce.repository;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByUserId(String userId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByUserIdAndStatus(String userId, OrderStatus status);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.createdAt >= :since")
    List<Order> findByUserIdAndCreatedAtAfter(String userId, LocalDateTime since);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.createdAt >= :since")
    long countByUserIdAndCreatedAtAfter(String userId, LocalDateTime since);
}
