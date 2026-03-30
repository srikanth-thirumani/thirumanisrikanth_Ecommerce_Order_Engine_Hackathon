package com.ecommerce.repository;

import com.ecommerce.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(String userId);

    Optional<CartItem> findByUserIdAndProductId(String userId, String productId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId")
    void deleteByUserId(String userId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId AND c.productId = :productId")
    void deleteByUserIdAndProductId(String userId, String productId);

    @Query("SELECT COUNT(c) FROM CartItem c WHERE c.userId = :userId")
    long countByUserId(String userId);
}
