package com.ecommerce.service.impl;

import com.ecommerce.audit.AuditLogger;
import com.ecommerce.cache.InMemoryCache;
import com.ecommerce.exception.DuplicateProductException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Microservice simulation: Product Service
 */
@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final InMemoryCache cache;
    private final AuditLogger auditLogger;

    @Value("${app.stock.low.threshold:5}")
    private int lowStockThreshold;

    public ProductServiceImpl(ProductRepository productRepository,
                              InMemoryCache cache,
                              AuditLogger auditLogger) {
        this.productRepository = productRepository;
        this.cache = cache;
        this.auditLogger = auditLogger;
    }

    @Override
    @Transactional
    public Product addProduct(String productId, String name, String description,
                              BigDecimal price, int stock, String category) {
        // Prevent duplicate ID
        if (productRepository.existsByProductId(productId)) {
            throw new DuplicateProductException(productId);
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }

        Product product = Product.builder()
                .productId(productId)
                .name(name)
                .description(description)
                .price(price)
                .totalStock(stock)
                .availableStock(stock)
                .reservedStock(0)
                .category(category)
                .build();

        Product saved = productRepository.save(product);
        cache.putProduct(saved);

        auditLogger.logSystem("PRODUCT_ADDED", "PRODUCT", productId,
                String.format("name=%s price=%.2f stock=%d", name, price.doubleValue(), stock));

        log.info("[PRODUCT] Added: {} - {} at ₹{} (stock={})", productId, name, price, stock);
        return saved;
    }

    @Override
    @Transactional
    public Product updateStock(String productId, int newStock) {
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        int oldStock = product.getTotalStock();
        int delta = newStock - oldStock;
        product.setTotalStock(newStock);
        product.setAvailableStock(Math.max(0, product.getAvailableStock() + delta));

        Product saved = productRepository.save(product);
        cache.putProduct(saved);

        auditLogger.logSystem("STOCK_UPDATED", "PRODUCT", productId,
                String.format("oldStock=%d newStock=%d", oldStock, newStock));

        log.info("[PRODUCT] Stock updated: {} -> {} units", productId, newStock);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProduct(String productId) {
        return cache.getProduct(productId)
                .orElseGet(() -> productRepository.findById(productId)
                        .orElseThrow(() -> new ProductNotFoundException(productId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        List<Product> products = productRepository.findAll();
        products.forEach(cache::putProduct);
        return products;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts(lowStockThreshold);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsProduct(String productId) {
        return productRepository.existsByProductId(productId);
    }
}
