package com.ecommerce.repository;

import com.ecommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId = :productId")
    Optional<Product> findByIdWithLock(String productId);

    @Query("SELECT p FROM Product p WHERE p.availableStock <= :threshold ORDER BY p.availableStock ASC")
    List<Product> findLowStockProducts(int threshold);

    @Query("SELECT p FROM Product p WHERE p.availableStock = 0")
    List<Product> findOutOfStockProducts();

    boolean existsByProductId(String productId);
}
