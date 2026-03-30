package com.ecommerce.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_reservations")
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "released")
    private boolean released;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    public StockReservation() {}

    @PrePersist
    protected void onCreate() {
        reservedAt = LocalDateTime.now();
        released = false;
    }

    public boolean isExpired() {
        return !released && LocalDateTime.now().isAfter(expiresAt);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public LocalDateTime getReservedAt() { return reservedAt; }
    public void setReservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isReleased() { return released; }
    public void setReleased(boolean released) { this.released = released; }
    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }

    public static StockReservationBuilder builder() {
        return new StockReservationBuilder();
    }

    public static class StockReservationBuilder {
        private String userId;
        private String productId;
        private int quantity;
        private LocalDateTime reservedAt;
        private LocalDateTime expiresAt;
        private boolean released;

        public StockReservationBuilder userId(String userId) { this.userId = userId; return this; }
        public StockReservationBuilder productId(String productId) { this.productId = productId; return this; }
        public StockReservationBuilder quantity(int quantity) { this.quantity = quantity; return this; }
        public StockReservationBuilder reservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; return this; }
        public StockReservationBuilder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public StockReservationBuilder released(boolean released) { this.released = released; return this; }

        public StockReservation build() {
            StockReservation s = new StockReservation();
            s.setUserId(userId);
            s.setProductId(productId);
            s.setQuantity(quantity);
            s.setReservedAt(reservedAt);
            s.setExpiresAt(expiresAt);
            s.setReleased(released);
            return s;
        }
    }
}
