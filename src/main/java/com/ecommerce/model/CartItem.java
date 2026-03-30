package com.ecommerce.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    public CartItem() {}

    public CartItem(Long id, String userId, String productId, String productName, int quantity, BigDecimal unitPrice, LocalDateTime addedAt) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.addedAt = addedAt;
    }

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public static CartItemBuilder builder() {
        return new CartItemBuilder();
    }

    public static class CartItemBuilder {
        private String userId;
        private String productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;

        public CartItemBuilder userId(String userId) { this.userId = userId; return this; }
        public CartItemBuilder productId(String productId) { this.productId = productId; return this; }
        public CartItemBuilder productName(String productName) { this.productName = productName; return this; }
        public CartItemBuilder quantity(int quantity) { this.quantity = quantity; return this; }
        public CartItemBuilder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }

        public CartItem build() {
            CartItem c = new CartItem();
            c.setUserId(userId);
            c.setProductId(productId);
            c.setProductName(productName);
            c.setQuantity(quantity);
            c.setUnitPrice(unitPrice);
            return c;
        }
    }
}
