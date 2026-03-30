package com.ecommerce.controller;

import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Product;
import com.ecommerce.service.CartService;
import com.ecommerce.service.InventoryService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RestDashboardController {

    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final InventoryService inventoryService;

    public RestDashboardController(ProductService productService,
                                   CartService cartService,
                                   OrderService orderService,
                                   InventoryService inventoryService) {
        this.productService = productService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/products")
    public List<Product> getProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/cart/{userId}")
    public List<CartItem> getCart(@PathVariable String userId) {
        return cartService.getCart(userId);
    }

    @PostMapping("/cart/{userId}/add")
    public CartItem addToCart(@PathVariable String userId, @RequestBody Map<String, Object> payload) {
        String productId = (String) payload.get("productId");
        int quantity = (int) payload.get("quantity");
        return cartService.addToCart(userId, productId, quantity);
    }

    @DeleteMapping("/cart/{userId}/remove/{productId}")
    public void removeFromCart(@PathVariable String userId, @PathVariable String productId) {
        cartService.removeFromCart(userId, productId);
    }

    @PostMapping("/orders/place")
    public Order placeOrder(@RequestBody Map<String, String> payload) {
        String userId = payload.get("userId");
        String method = payload.get("paymentMethod");
        String idempotencyKey = payload.get("idempotencyKey");
        return orderService.placeOrder(userId, method, idempotencyKey);
    }

    @GetMapping("/orders/{userId}")
    public List<Order> getUserOrders(@PathVariable String userId) {
        return orderService.getOrdersByUser(userId);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public Order cancelOrder(@PathVariable String orderId, @RequestParam String userId) {
        return orderService.cancelOrder(orderId, userId);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Order> allOrders = orderService.getAllOrders();
        stats.put("totalOrders", allOrders.size());
        stats.put("paidOrders", allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PAID).count());
        stats.put("failedOrders", allOrders.stream().filter(o -> o.getStatus() == OrderStatus.FAILED).count());
        stats.put("totalProducts", productService.getAllProducts().size());
        return stats;
    }
}
