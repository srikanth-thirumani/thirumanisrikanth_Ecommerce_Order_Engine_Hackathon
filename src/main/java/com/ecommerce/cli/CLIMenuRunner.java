package com.ecommerce.cli;

import com.ecommerce.audit.AuditLogger;
import com.ecommerce.cache.InMemoryCache;
import com.ecommerce.model.*;
import com.ecommerce.payment.PaymentProcessorFactory;
import com.ecommerce.service.CartService;
import com.ecommerce.service.InventoryService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.impl.ConcurrencySimulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * CLI Menu Interface — Full interactive terminal for the E-Commerce Order Engine.
 * All 20 features accessible through numbered menu options.
 */
@Component
public class CLIMenuRunner {

    private static final Logger log = LoggerFactory.getLogger(CLIMenuRunner.class);

    private static final String SEPARATOR = "═".repeat(65);
    private static final String THIN_SEP  = "─".repeat(65);

    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final AuditLogger auditLogger;
    private final InMemoryCache cache;
    private final ConcurrencySimulatorService concurrencySimulator;
    private final PaymentProcessorFactory paymentFactory;
    private final Scanner scanner;

    private String currentUserId = "USER_1";

    public CLIMenuRunner(ProductService productService,
                         CartService cartService,
                         OrderService orderService,
                         InventoryService inventoryService,
                         AuditLogger auditLogger,
                         InMemoryCache cache,
                         ConcurrencySimulatorService concurrencySimulator,
                         PaymentProcessorFactory paymentFactory,
                         Scanner scanner) {
        this.productService = productService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.auditLogger = auditLogger;
        this.cache = cache;
        this.concurrencySimulator = concurrencySimulator;
        this.paymentFactory = paymentFactory;
        this.scanner = scanner;
    }

    public void run() {
        printBanner();
        seedSampleData();

        boolean running = true;
        while (running) {
            printMainMenu();
            String input = prompt("Enter choice");
            try {
                switch (input.trim()) {
                    case "1"  -> handleAddProduct();
                    case "2"  -> handleViewProducts();
                    case "3"  -> handleAddToCart();
                    case "4"  -> handleRemoveFromCart();
                    case "5"  -> handleViewCart();
                    case "6"  -> handleApplyCoupon();
                    case "7"  -> handlePlaceOrder();
                    case "8"  -> handleCancelOrder();
                    case "9"  -> handleViewOrders();
                    case "10" -> handleLowStockAlert();
                    case "11" -> handleReturnProduct();
                    case "12" -> handleConcurrentSimulation();
                    case "13" -> handleViewLogs();
                    case "14" -> handleFailureMode();
                    case "15" -> handleSwitchUser();
                    case "16" -> handleAdvanceOrderState();
                    case "0"  -> { running = false; handleExit(); }
                    default   -> printError("Invalid option. Please choose 0-16.");
                }
            } catch (Exception e) {
                printError("Error: " + e.getMessage());
                log.error("[CLI] Unhandled error", e);
            }
            if (running) pauseForUser();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 1. Add Product
    // ─────────────────────────────────────────────────────────────
    private void handleAddProduct() {
        printHeader("ADD PRODUCT");
        String id    = prompt("Product ID (e.g. P001)");
        String name  = prompt("Product Name");
        String desc  = prompt("Description");
        String priceStr = prompt("Price (₹)");
        String stockStr = prompt("Stock quantity");
        String cat   = prompt("Category");

        try {
            BigDecimal price = new BigDecimal(priceStr);
            int stock = Integer.parseInt(stockStr);
            Product p = productService.addProduct(id, name, desc, price, stock, cat);
            printSuccess(String.format("Product added: [%s] %s | ₹%.2f | Stock: %d",
                    p.getProductId(), p.getName(), p.getPrice(), p.getAvailableStock()));
        } catch (NumberFormatException e) {
            printError("Invalid price or stock. Please enter numeric values.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 2. View Products
    // ─────────────────────────────────────────────────────────────
    private void handleViewProducts() {
        printHeader("ALL PRODUCTS");
        List<Product> products = productService.getAllProducts();
        if (products.isEmpty()) {
            printInfo("No products found.");
            return;
        }
        System.out.printf("%-10s %-25s %-12s %-10s %-10s %-10s%n",
                "ID", "Name", "Category", "Price(₹)", "Available", "Reserved");
        System.out.println(THIN_SEP);
        for (Product p : products) {
            String stockWarning = p.getAvailableStock() == 0 ? " [OUT OF STOCK]"
                    : p.getAvailableStock() <= 5 ? " [LOW]" : "";
            System.out.printf("%-10s %-25s %-12s %-10.2f %-10d %-10d%s%n",
                    p.getProductId(), p.getName(), p.getCategory(),
                    p.getPrice(), p.getAvailableStock(), p.getReservedStock(), stockWarning);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 3. Add to Cart
    // ─────────────────────────────────────────────────────────────
    private void handleAddToCart() {
        printHeader("ADD TO CART");
        printInfo("Current User: " + currentUserId);
        handleViewProducts();
        String productId = prompt("Product ID");
        String qtyStr = prompt("Quantity");

        try {
            int qty = Integer.parseInt(qtyStr);
            CartItem item = cartService.addToCart(currentUserId, productId, qty);
            printSuccess(String.format("Added to cart: %s x%d @ ₹%.2f each = ₹%.2f",
                    item.getProductName(), item.getQuantity(),
                    item.getUnitPrice(), item.getSubtotal()));
        } catch (NumberFormatException e) {
            printError("Invalid quantity.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 4. Remove from Cart
    // ─────────────────────────────────────────────────────────────
    private void handleRemoveFromCart() {
        printHeader("REMOVE FROM CART");
        printInfo("Current User: " + currentUserId);
        List<CartItem> cart = cartService.getCart(currentUserId);
        if (cart.isEmpty()) { printInfo("Your cart is empty."); return; }
        printCartTable(cart);
        String productId = prompt("Product ID to remove");
        cartService.removeFromCart(currentUserId, productId);
        printSuccess("Item removed and stock reservation released: " + productId);
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 5. View Cart
    // ─────────────────────────────────────────────────────────────
    private void handleViewCart() {
        printHeader("MY CART — User: " + currentUserId);
        List<CartItem> cart = cartService.getCart(currentUserId);
        if (cart.isEmpty()) {
            printInfo("Your cart is empty.");
            return;
        }
        printCartTable(cart);

        BigDecimal subtotal = cart.stream().map(CartItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        String coupon = cartService.getAppliedCoupon(currentUserId);
        System.out.println(THIN_SEP);
        System.out.printf("  Subtotal: ₹%.2f%n", subtotal);
        if (coupon != null) System.out.println("  Coupon Applied: " + coupon);
        System.out.println(THIN_SEP);

        // Show active reservations
        List<StockReservation> reservations = inventoryService.getActiveReservations(currentUserId);
        if (!reservations.isEmpty()) {
            System.out.println("\n  Active Reservations:");
            for (StockReservation r : reservations) {
                System.out.printf("    %s x%d — expires %s%n",
                        r.getProductId(), r.getQuantity(), r.getExpiresAt().toString().substring(0, 16));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 6. Apply Coupon
    // ─────────────────────────────────────────────────────────────
    private void handleApplyCoupon() {
        printHeader("APPLY COUPON");
        System.out.println("  Available coupons:");
        System.out.println("  SAVE10  → 10% off");
        System.out.println("  FLAT200 → ₹200 off (min order ₹500)");
        System.out.println("  EXTRA5  → 5% off");
        System.out.println(THIN_SEP);
        String coupon = prompt("Enter coupon code");
        cartService.applyCoupon(currentUserId, coupon.toUpperCase());
        printSuccess("Coupon applied: " + coupon.toUpperCase());
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 7. Place Order
    // ─────────────────────────────────────────────────────────────
    private void handlePlaceOrder() {
        printHeader("PLACE ORDER — User: " + currentUserId);
        List<CartItem> cart = cartService.getCart(currentUserId);
        if (cart.isEmpty()) { printInfo("Cart is empty. Add items first."); return; }

        printCartTable(cart);
        System.out.println("\n  Payment Methods: UPI / CARD / COD");
        String method = prompt("Choose payment method").toUpperCase();

        System.out.println("\n  [Generating idempotency key...]");
        String idempotencyKey = "IDEM-" + currentUserId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        if (paymentFactory.isFailureInjected()) {
            printWarning("⚠ FAILURE MODE is active. Payment likely to fail.");
        }

        try {
            System.out.println("\n  Processing order...");
            Order order = orderService.placeOrder(currentUserId, method, idempotencyKey);
            System.out.println(SEPARATOR);
            printSuccess("🎉 ORDER PLACED SUCCESSFULLY!");
            System.out.printf("  Order ID    : %s%n", order.getOrderId());
            System.out.printf("  Status      : %s%n", order.getStatus());
            System.out.printf("  Subtotal    : ₹%.2f%n", order.getSubtotal());
            System.out.printf("  Discount    : ₹%.2f%n", order.getDiscountAmount());
            System.out.printf("  Total Paid  : ₹%.2f%n", order.getTotalAmount());
            System.out.printf("  Method      : %s%n", order.getPaymentMethod());
            if (order.getCouponCode() != null) {
                System.out.printf("  Coupon      : %s%n", order.getCouponCode());
            }
            System.out.println(SEPARATOR);
        } catch (Exception e) {
            printError("Order failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 8. Cancel Order
    // ─────────────────────────────────────────────────────────────
    private void handleCancelOrder() {
        printHeader("CANCEL ORDER");
        printInfo("Current User: " + currentUserId);
        List<Order> orders = orderService.getOrdersByUser(currentUserId);
        if (orders.isEmpty()) { printInfo("No orders found."); return; }

        printOrderList(orders);
        String orderId = prompt("Order ID to cancel");

        try {
            Order cancelled = orderService.cancelOrder(orderId, currentUserId);
            printSuccess("Order cancelled: " + cancelled.getOrderId() + " | Status: " + cancelled.getStatus());
        } catch (Exception e) {
            printError("Cancellation failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 9. View Orders
    // ─────────────────────────────────────────────────────────────
    private void handleViewOrders() {
        printHeader("ORDER MANAGEMENT");
        System.out.println("  Filter: A=All  U=My Orders  P=PAID  F=FAILED  C=CANCELLED");
        String filter = prompt("Filter choice").toUpperCase();

        List<Order> orders;
        orders = switch (filter) {
            case "U" -> orderService.getOrdersByUser(currentUserId);
            case "P" -> orderService.getOrdersByStatus(OrderStatus.PAID);
            case "F" -> orderService.getOrdersByStatus(OrderStatus.FAILED);
            case "C" -> orderService.getOrdersByStatus(OrderStatus.CANCELLED);
            default  -> orderService.getAllOrders();
        };

        if (orders.isEmpty()) { printInfo("No orders found."); return; }

        System.out.println("\n  Search by ID? (leave blank to show all)");
        String searchId = prompt("Order ID (or Enter to skip)").trim();

        if (!searchId.isEmpty()) {
            orders = orders.stream()
                    .filter(o -> o.getOrderId().equalsIgnoreCase(searchId))
                    .toList();
        }

        printOrderList(orders);

        // Offer detail view
        if (!orders.isEmpty()) {
            String detailId = prompt("\nEnter Order ID for details (or Enter to skip)").trim();
            if (!detailId.isEmpty()) {
                try {
                    Order o = orderService.getOrder(detailId);
                    printOrderDetail(o);
                } catch (Exception e) {
                    printError(e.getMessage());
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 10. Low Stock Alert
    // ─────────────────────────────────────────────────────────────
    private void handleLowStockAlert() {
        printHeader("INVENTORY ALERT — LOW STOCK");
        List<Product> lowStock = productService.getLowStockProducts();
        if (lowStock.isEmpty()) {
            printSuccess("All products have healthy stock levels.");
            return;
        }
        System.out.printf("%-10s %-25s %-12s %-10s%n", "ID", "Name", "Available", "Status");
        System.out.println(THIN_SEP);
        for (Product p : lowStock) {
            String status = p.getAvailableStock() == 0 ? "❌ OUT OF STOCK" : "⚠ LOW STOCK";
            System.out.printf("%-10s %-25s %-12d %s%n",
                    p.getProductId(), p.getName(), p.getAvailableStock(), status);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 11. Return Product
    // ─────────────────────────────────────────────────────────────
    private void handleReturnProduct() {
        printHeader("RETURN & REFUND");
        printInfo("Current User: " + currentUserId);
        List<Order> paidOrders = orderService.getOrdersByUser(currentUserId).stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID
                        || o.getStatus() == OrderStatus.DELIVERED
                        || o.getStatus() == OrderStatus.SHIPPED)
                .toList();

        if (paidOrders.isEmpty()) { printInfo("No eligible orders for return."); return; }

        printOrderList(paidOrders);
        String orderId = prompt("Order ID");

        try {
            Order order = orderService.getOrder(orderId);
            System.out.println("\n  Items in order:");
            for (OrderItem item : order.getItems()) {
                System.out.printf("    [%s] %s x%d (returnable: %d)%n",
                        item.getProductId(), item.getProductName(),
                        item.getQuantity(), item.getReturnableQuantity());
            }

            String productId = prompt("Product ID to return");
            String qtyStr = prompt("Return quantity");
            int returnQty = Integer.parseInt(qtyStr);

            Order updated = orderService.returnItems(orderId, productId, returnQty);
            BigDecimal refund = updated.getItems().stream()
                    .filter(i -> i.getProductId().equals(productId))
                    .findFirst()
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(returnQty)))
                    .orElse(BigDecimal.ZERO);

            printSuccess(String.format("Return processed! Refund: ₹%.2f | Order status: %s",
                    refund, updated.getStatus()));
        } catch (Exception e) {
            printError("Return failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 12. Simulate Concurrent Users
    // ─────────────────────────────────────────────────────────────
    private void handleConcurrentSimulation() {
        printHeader("CONCURRENT USER SIMULATION");
        System.out.println("  This simulates multiple users trying to buy the same limited-stock product.");
        System.out.println("  ReentrantLock prevents overselling — only valid orders succeed.\n");

        handleViewProducts();
        String productId = prompt("Product ID to use for simulation");
        String userCountStr = prompt("Number of concurrent users (e.g. 5)");
        String qtyStr = prompt("Quantity each user tries to buy (e.g. 1)");
        String method = prompt("Payment method (UPI/CARD/COD)").toUpperCase();

        try {
            int userCount = Integer.parseInt(userCountStr);
            int qty = Integer.parseInt(qtyStr);

            System.out.println("\n  🚀 Starting concurrent simulation...\n");
            ConcurrencySimulatorService.SimulationResult result =
                    concurrencySimulator.simulate(userCount, productId, qty, method);

            System.out.println(SEPARATOR);
            System.out.println("  SIMULATION RESULTS");
            System.out.println(THIN_SEP);
            System.out.printf("  Total Users       : %d%n", result.totalUsers());
            System.out.printf("  Successful Orders : %d ✓%n", result.successCount());
            System.out.printf("  Failed Attempts   : %d ✗%n", result.failureCount());
            System.out.printf("  Final Stock Left  : %d%n", result.finalStock());
            System.out.println(THIN_SEP);
            System.out.println("  Individual Results:");
            result.details().forEach(d -> System.out.println("    " + d));
            System.out.println(SEPARATOR);
        } catch (NumberFormatException e) {
            printError("Invalid number input.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 13. View Logs
    // ─────────────────────────────────────────────────────────────
    private void handleViewLogs() {
        printHeader("AUDIT LOG VIEWER");
        System.out.println("  Filter: A=All  U=By User  R=Recent 10 min");
        String filter = prompt("Filter").toUpperCase();

        List<com.ecommerce.model.AuditLog> logs;
        if ("U".equals(filter)) {
            String uid = prompt("User ID (default: " + currentUserId + ")").trim();
            if (uid.isEmpty()) uid = currentUserId;
            logs = auditLogger.getLogsByUser(uid);
        } else if ("R".equals(filter)) {
            logs = auditLogger.getRecentLogs(10);
        } else {
            logs = auditLogger.getAllLogs();
        }

        if (logs.isEmpty()) { printInfo("No audit logs found."); return; }

        System.out.println(THIN_SEP);
        // Show last 50 max
        logs.stream().limit(50).forEach(l -> System.out.println("  " + auditLogger.formatLog(l)));
        System.out.printf("%n  Total: %d log entries shown (max 50).%n", Math.min(logs.size(), 50));
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 14. Trigger Failure Mode
    // ─────────────────────────────────────────────────────────────
    private void handleFailureMode() {
        printHeader("FAILURE INJECTION MODE");
        boolean current = paymentFactory.isFailureInjected();
        System.out.printf("  Current mode: %s%n", current ? "⚠ FAILURE ACTIVE" : "✓ NORMAL");
        System.out.println("  1 = Enable failure injection (payments will fail)");
        System.out.println("  2 = Disable failure injection (normal mode)");
        System.out.println("  3 = Simulate random inventory failure");
        String choice = prompt("Choice");

        switch (choice) {
            case "1" -> {
                paymentFactory.setFailureInjected(true);
                printWarning("⚠ Failure injection ENABLED. All payments will now fail.");
                auditLogger.logSystem("FAILURE_INJECTION_ENABLED", "SYSTEM", "PAYMENT", "mode=FAILURE");
            }
            case "2" -> {
                paymentFactory.setFailureInjected(false);
                printSuccess("✓ Failure injection DISABLED. System running normally.");
                auditLogger.logSystem("FAILURE_INJECTION_DISABLED", "SYSTEM", "PAYMENT", "mode=NORMAL");
            }
            case "3" -> {
                printWarning("Simulating random inventory failure...");
                try {
                    inventoryService.processExpiredReservations();
                    printSuccess("Inventory maintenance run completed.");
                } catch (Exception e) {
                    printError("Inventory failure: " + e.getMessage());
                }
            }
            default -> printError("Invalid choice.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 15. Switch User
    // ─────────────────────────────────────────────────────────────
    private void handleSwitchUser() {
        printHeader("SWITCH USER");
        System.out.println("  Current user: " + currentUserId);
        System.out.println("  Pre-defined users: USER_1, USER_2, USER_3");
        String newUser = prompt("Enter user ID").trim();
        if (!newUser.isEmpty()) {
            currentUserId = newUser;
            printSuccess("Switched to user: " + currentUserId);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MENU HANDLER: 16. Advance Order State
    // ─────────────────────────────────────────────────────────────
    private void handleAdvanceOrderState() {
        printHeader("ADVANCE ORDER STATE");
        printInfo("Valid transitions: PAID→SHIPPED, SHIPPED→DELIVERED");
        String orderId = prompt("Order ID");
        System.out.println("  States: SHIPPED, DELIVERED");
        String stateStr = prompt("Target state").toUpperCase();
        try {
            OrderStatus target = OrderStatus.valueOf(stateStr);
            orderService.advanceOrderState(orderId, target);
            printSuccess("Order " + orderId + " advanced to " + target);
        } catch (IllegalArgumentException e) {
            printError("Invalid status: " + stateStr);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SAMPLE DATA SEEDING
    // ─────────────────────────────────────────────────────────────
    private void seedSampleData() {
        try {
            if (!productService.existsProduct("P001")) {
                productService.addProduct("P001", "iPhone 15 Pro",     "Apple flagship phone",       new BigDecimal("99999"), 10, "Electronics");
                productService.addProduct("P002", "Samsung Galaxy S24", "Android flagship phone",     new BigDecimal("79999"),  5, "Electronics");
                productService.addProduct("P003", "Sony WH-1000XM5",   "Noise cancelling headphones", new BigDecimal("24999"), 20, "Audio");
                productService.addProduct("P004", "Nike Air Max 270",  "Running shoes",               new BigDecimal("8999"),   3, "Footwear");
                productService.addProduct("P005", "Kindle Paperwhite", "E-reader 16GB",               new BigDecimal("12999"),  2, "Books");
                log.info("[SEED] Sample products loaded.");
            }
        } catch (Exception e) {
            log.warn("[SEED] Sample data already exists or error: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────
    private void printBanner() {
        System.out.println("\n" + SEPARATOR);
        System.out.println("  ██████╗ ██████╗ ██████╗ ███████╗██████╗     ENGINE");
        System.out.println("  ██╔═══╝ ██╔══╝ ██╔══╝  ██╔════╝██╔══╝");
        System.out.println("  ██║ ██╗ ██████╗ ██████╗ █████╗  ██████╗");
        System.out.println("  ██║ ██║ ██╔═══╝ ██╔═══╝ ██╔══╝  ██╔══╝");
        System.out.println("  ██████║ ██║     ██║     ███████╗██║");
        System.out.println("  ╚═════╝ ╚═╝     ╚═╝     ╚══════╝╚═╝     v1.0");
        System.out.println(SEPARATOR);
        System.out.println("  Distributed E-Commerce Order Engine");
        System.out.println("  Java 17 + Spring Boot + H2 + JPA + Multithreading");
        System.out.println(SEPARATOR + "\n");
    }

    private void printMainMenu() {
        System.out.println("\n" + SEPARATOR);
        System.out.println("  MAIN MENU  [User: " + currentUserId + "]" +
                (paymentFactory.isFailureInjected() ? "  ⚠ FAILURE MODE ON" : ""));
        System.out.println(SEPARATOR);
        System.out.println("  PRODUCTS & CATALOG");
        System.out.println("   1. Add Product           2. View All Products");
        System.out.println("   10. Low Stock Alert");
        System.out.println();
        System.out.println("  CART OPERATIONS");
        System.out.println("   3. Add to Cart           4. Remove from Cart");
        System.out.println("   5. View Cart             6. Apply Coupon");
        System.out.println();
        System.out.println("  ORDER MANAGEMENT");
        System.out.println("   7. Place Order           8. Cancel Order");
        System.out.println("   9. View Orders           11. Return Product");
        System.out.println("   16. Advance Order State");
        System.out.println();
        System.out.println("  SYSTEM & ADMIN");
        System.out.println("   12. Simulate Concurrent Users");
        System.out.println("   13. View Audit Logs");
        System.out.println("   14. Toggle Failure Mode");
        System.out.println("   15. Switch User");
        System.out.println("   0.  Exit");
        System.out.println(SEPARATOR);
    }

    private void printCartTable(List<CartItem> cart) {
        System.out.printf("  %-10s %-22s %-8s %-10s %-12s%n",
                "ProdID", "Name", "Qty", "Unit(₹)", "Subtotal(₹)");
        System.out.println("  " + THIN_SEP.substring(0, 62));
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem c : cart) {
            System.out.printf("  %-10s %-22s %-8d %-10.2f %-12.2f%n",
                    c.getProductId(), c.getProductName(), c.getQuantity(),
                    c.getUnitPrice(), c.getSubtotal());
            total = total.add(c.getSubtotal());
        }
        System.out.printf("  %50s TOTAL: ₹%.2f%n", "", total);
    }

    private void printOrderList(List<Order> orders) {
        System.out.printf("  %-22s %-12s %-14s %-12s %-8s%n",
                "Order ID", "Status", "Total(₹)", "Method", "Date");
        System.out.println("  " + THIN_SEP.substring(0, 62));
        for (Order o : orders) {
            System.out.printf("  %-22s %-12s %-14.2f %-12s %-8s%n",
                    o.getOrderId(), o.getStatus(),
                    o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO,
                    o.getPaymentMethod(),
                    o.getCreatedAt().toString().substring(0, 10));
        }
    }

    private void printOrderDetail(Order o) {
        System.out.println("\n  " + SEPARATOR);
        System.out.println("  ORDER DETAIL: " + o.getOrderId());
        System.out.println("  " + THIN_SEP);
        System.out.printf("  Status    : %s%n", o.getStatus());
        System.out.printf("  User      : %s%n", o.getUserId());
        System.out.printf("  Subtotal  : ₹%.2f%n", o.getSubtotal());
        System.out.printf("  Discount  : ₹%.2f%s%n", o.getDiscountAmount(),
                o.getCouponCode() != null ? " (coupon: " + o.getCouponCode() + ")" : "");
        System.out.printf("  Total     : ₹%.2f%n", o.getTotalAmount());
        System.out.printf("  Method    : %s%n", o.getPaymentMethod());
        System.out.printf("  Created   : %s%n", o.getCreatedAt());
        if (o.getFailureReason() != null) {
            System.out.printf("  Failure   : %s%n", o.getFailureReason());
        }
        System.out.println("  " + THIN_SEP);
        System.out.println("  Items:");
        for (OrderItem item : o.getItems()) {
            System.out.printf("    [%s] %s x%d @ ₹%.2f = ₹%.2f (returned: %d)%n",
                    item.getProductId(), item.getProductName(), item.getQuantity(),
                    item.getUnitPrice(), item.getSubtotal(), item.getReturnedQuantity());
        }
        System.out.println("  " + SEPARATOR);
    }

    private String prompt(String label) {
        System.out.print("  >> " + label + ": ");
        return scanner.nextLine();
    }

    private void printHeader(String title) {
        System.out.println("\n" + SEPARATOR);
        System.out.println("  ● " + title);
        System.out.println(SEPARATOR);
    }

    private void printSuccess(String msg) {
        System.out.println("\n  ✅  " + msg);
    }

    private void printError(String msg) {
        System.out.println("\n  ❌  " + msg);
    }

    private void printInfo(String msg) {
        System.out.println("  ℹ  " + msg);
    }

    private void printWarning(String msg) {
        System.out.println("\n  ⚠  " + msg);
    }

    private void pauseForUser() {
        System.out.print("\n  [Press Enter to continue...]");
        scanner.nextLine();
    }

    private void handleExit() {
        System.out.println("\n" + SEPARATOR);
        System.out.println("  Thank you for using E-Commerce Order Engine!");
        System.out.println("  Shutting down gracefully...");
        System.out.println(SEPARATOR + "\n");
    }
}
