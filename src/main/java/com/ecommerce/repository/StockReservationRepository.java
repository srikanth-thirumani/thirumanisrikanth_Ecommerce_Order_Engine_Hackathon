package com.ecommerce.repository;

import com.ecommerce.model.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByUserIdAndReleasedFalse(String userId);

    Optional<StockReservation> findByUserIdAndProductIdAndReleasedFalse(String userId, String productId);

    @Query("SELECT s FROM StockReservation s WHERE s.released = false AND s.expiresAt < :now")
    List<StockReservation> findExpiredReservations(LocalDateTime now);

    @Modifying
    @Query("UPDATE StockReservation s SET s.released = true, s.releasedAt = :now WHERE s.userId = :userId AND s.released = false")
    void releaseAllByUserId(String userId, LocalDateTime now);
}
