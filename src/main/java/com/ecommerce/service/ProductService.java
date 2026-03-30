package com.ecommerce.service;

import com.ecommerce.model.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product Service — Microservice simulation: Product Service module
 */
public interface ProductService {
    Product addProduct(String productId, String name, String description,
                       BigDecimal price, int stock, String category);
    Product updateStock(String productId, int newStock);
    Product getProduct(String productId);
    List<Product> getAllProducts();
    List<Product> getLowStockProducts();
    boolean existsProduct(String productId);
}
