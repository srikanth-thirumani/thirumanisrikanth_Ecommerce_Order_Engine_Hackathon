package com.ecommerce.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "returned_quantity")
    private int returnedQuantity;

    public OrderItem() {}

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public int getReturnableQuantity() {
        return quantity - returnedQuantity;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getReturnedQuantity() { return returnedQuantity; }
    public void setReturnedQuantity(int returnedQuantity) { this.returnedQuantity = returnedQuantity; }

    public static OrderItemBuilder builder() {
        return new OrderItemBuilder();
    }

    public static class OrderItemBuilder {
        private Order order;
        private String productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private int returnedQuantity;

        public OrderItemBuilder order(Order order) { this.order = order; return this; }
        public OrderItemBuilder productId(String productId) { this.productId = productId; return this; }
        public OrderItemBuilder productName(String productName) { this.productName = productName; return this; }
        public OrderItemBuilder quantity(int quantity) { this.quantity = quantity; return this; }
        public OrderItemBuilder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public OrderItemBuilder returnedQuantity(int returnedQuantity) { this.returnedQuantity = returnedQuantity; return this; }

        public OrderItem build() {
            OrderItem i = new OrderItem();
            i.setOrder(order);
            i.setProductId(productId);
            i.setProductName(productName);
            i.setQuantity(quantity);
            i.setUnitPrice(unitPrice);
            i.setReturnedQuantity(returnedQuantity);
            return i;
        }
    }
}
