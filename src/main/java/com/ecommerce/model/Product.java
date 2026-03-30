package com.ecommerce.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "product_id", nullable = false, unique = true)
    private String productId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "total_stock", nullable = false)
    private int totalStock;

    @Column(name = "available_stock", nullable = false)
    private int availableStock;

    @Column(name = "reserved_stock", nullable = false)
    private int reservedStock;

    @Column(name = "category")
    private String category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public Product() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (availableStock == 0 && totalStock > 0) {
            availableStock = totalStock;
        }
        reservedStock = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean hasAvailableStock(int quantity) {
        return availableStock >= quantity;
    }

    public void reserveStock(int quantity) {
        if (availableStock < quantity) {
            throw new IllegalStateException("Insufficient stock for product: " + productId);
        }
        availableStock -= quantity;
        reservedStock += quantity;
    }

    public void releaseStock(int quantity) {
        reservedStock -= quantity;
        availableStock += quantity;
        if (reservedStock < 0) reservedStock = 0;
    }

    public void deductReservedStock(int quantity) {
        if (reservedStock < quantity) {
            throw new IllegalStateException("Reserved stock insufficient for deduction: " + productId);
        }
        reservedStock -= quantity;
        totalStock -= quantity;
    }

    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getTotalStock() { return totalStock; }
    public void setTotalStock(int totalStock) { this.totalStock = totalStock; }
    public int getAvailableStock() { return availableStock; }
    public void setAvailableStock(int availableStock) { this.availableStock = availableStock; }
    public int getReservedStock() { return reservedStock; }
    public void setReservedStock(int reservedStock) { this.reservedStock = reservedStock; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public static ProductBuilder builder() {
        return new ProductBuilder();
    }

    public static class ProductBuilder {
        private String productId;
        private String name;
        private String description;
        private BigDecimal price;
        private int totalStock;
        private int availableStock;
        private int reservedStock;
        private String category;

        public ProductBuilder productId(String productId) { this.productId = productId; return this; }
        public ProductBuilder name(String name) { this.name = name; return this; }
        public ProductBuilder description(String description) { this.description = description; return this; }
        public ProductBuilder price(BigDecimal price) { this.price = price; return this; }
        public ProductBuilder totalStock(int totalStock) { this.totalStock = totalStock; return this; }
        public ProductBuilder availableStock(int availableStock) { this.availableStock = availableStock; return this; }
        public ProductBuilder reservedStock(int reservedStock) { this.reservedStock = reservedStock; return this; }
        public ProductBuilder category(String category) { this.category = category; return this; }

        public Product build() {
            Product p = new Product();
            p.setProductId(productId);
            p.setName(name);
            p.setDescription(description);
            p.setPrice(price);
            p.setTotalStock(totalStock);
            p.setAvailableStock(availableStock);
            p.setReservedStock(reservedStock);
            p.setCategory(category);
            return p;
        }
    }
}
