// Dashboard Logic for E-Commerce Order Engine
const API_URL = '/api';
let currentUserId = 'USER_1';
let isFailureMode = false;

// Initialize dashboard components
document.addEventListener('DOMContentLoaded', () => {
  setupNavigation();
  loadStats();
  loadRecentOrders();
  loadProducts();
});

// Setup navigation between dashboard, products, and orders views
function setupNavigation() {
  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      
      // Update UI state for sidebar items
      document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
      item.classList.add('active');
      
      // Switch view containers
      const view = item.getAttribute('data-view');
      document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
      document.getElementById(`${view}-view`).classList.add('active');
      
      // Update header title based on view
      const viewTitle = view.charAt(0).toUpperCase() + view.slice(1);
      document.getElementById('view-title').innerText = viewTitle;
      
      // Refresh context-specific data
      if (view === 'dashboard') loadStats();
      if (view === 'products') loadProducts();
      if (view === 'orders') loadAllOrders();
    });
  });
}

// Fetch global stats and update dashboard counters
async function loadStats() {
  try {
    const response = await fetch(`${API_URL}/stats`);
    const stats = await response.json();
    
    document.getElementById('stat-paid-orders').innerText = stats.paidOrders;
    document.getElementById('stat-failed-orders').innerText = stats.failedOrders;
    document.getElementById('stat-total-products').innerText = stats.totalProducts;
    
    // Calculate simulated revenue from total successful orders
    const revenue = stats.paidOrders * 12500.50; // Average order value simulation
    document.getElementById('stat-revenue').innerText = `₹${revenue.toLocaleString()}`;
    
    // Performance state alerts based on failed/paid order ratio
    const failureWarning = document.getElementById('failure-warning');
    if (stats.failedOrders > 0) {
      failureWarning.innerText = `⚠ ${stats.failedOrders} issues detected`;
      failureWarning.style.color = 'var(--accent-danger)';
    } else {
      failureWarning.innerText = 'System Healthy';
      failureWarning.style.color = 'var(--accent-success)';
    }
  } catch (error) {
    console.error('Error loading stats:', error);
  }
}

// Populate product grid with dynamic item cards
async function loadProducts() {
  const container = document.getElementById('products-container');
  if (!container) return;
  
  try {
    const response = await fetch(`${API_URL}/products`);
    const products = await response.json();
    
    container.innerHTML = products.map(product => `
      <div class="product-card">
        <div style="display: flex; justify-content: space-between; align-items: flex-start;">
          <h3 style="font-weight: 700; color: white;">${product.name}</h3>
          <span class="stock-badge ${product.availableStock > 5 ? 'stock-good' : 'stock-low'}">
            Stock: ${product.availableStock}
          </span>
        </div>
        <p style="color: var(--text-secondary); font-size: 0.8rem; margin-top: 5px;">${product.description}</p>
        <div class="product-price">₹${product.price ? product.price.toLocaleString() : '0.00'}</div>
        <button class="btn btn-primary" style="width: 100%; border-radius: 14px;" onclick="addToCart('${product.productId}')">
          <i class="fas fa-cart-plus"></i> Buy Now
        </button>
      </div>
    `).join('');
  } catch (error) {
    console.error('Error loading products:', error);
  }
}

// Add an item to the shopping cart via backend API
async function addToCart(productId) {
  const idempotencyKey = `PAY-${Date.now()}`;
  try {
    // 1. Direct simulation: Place order for this product
    const orderData = {
      userId: currentUserId,
      paymentMethod: 'UPI',
      idempotencyKey: idempotencyKey
    };
    
    // In our simplified dashboard, clicking 'Buy Now' attempts to place an order directly for simplicity in demo
    // The engine's atomic transaction logic handles stock check, fraud check, and payment processing
    const response = await fetch(`${API_URL}/orders/place`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(orderData)
    });
    
    const result = await response.json();
    
    if (result.status === 'PAID') {
      alert(`Order Success! ID: ${result.orderId}\nTotal: ₹${result.totalAmount}`);
    } else {
      alert(`Order Failed: ${result.failureReason || 'Stock or Payment error'}`);
    }
    
    loadStats();
    loadProducts();
  } catch (error) {
    console.error('Error during quick purchase:', error);
    alert('System error - check console logs.');
  }
}

// Fetch and display all historical orders in a unified table view
async function loadAllOrders() {
  const container = document.getElementById('all-orders-body');
  if (!container) return;
  
  try {
    const response = await fetch(`${API_URL}/orders/${currentUserId}`);
    const orders = await response.json();
    
    container.innerHTML = orders.map(order => `
      <tr>
        <td style="font-family: monospace; font-size: 0.85rem;">${order.orderId}</td>
        <td><span class="${order.status === 'PAID' ? 'status-paid' : 'status-failed'}">${order.status}</span></td>
        <td><i class="fas fa-credit-card"></i> ${order.paymentMethod}</td>
        <td style="font-weight: 700;">₹${order.totalAmount ? order.totalAmount.toLocaleString() : '0'}</td>
        <td style="color: var(--text-secondary); font-size: 0.8rem;">${order.items ? order.items.length : 0} items</td>
        <td>
          <button class="btn" style="padding: 6px 12px; background: rgba(255,255,255,0.05); color: white;" onclick="cancelOrder('${order.orderId}')">
            <i class="fas fa-times"></i>
          </button>
        </td>
      </tr>
    `).join('');
  } catch (error) {
    console.error('Error loading all orders:', error);
  }
}

// Cancel an active order using the backend engine service
async function cancelOrder(orderId) {
  if (!confirm('Are you sure you want to cancel this order and refund stock?')) return;
  
  try {
    const response = await fetch(`${API_URL}/orders/${orderId}/cancel?userId=${currentUserId}`, {
      method: 'POST'
    });
    const order = await response.json();
    alert(`Order ${orderId} has been CANCELLED and stock restored.`);
    loadAllOrders();
    loadStats();
  } catch (error) {
    alert('Cancellation failed.');
  }
}

// Simulation function to toggle error injection via dashboard button
function toggleFailureMode() {
  isFailureMode = !isFailureMode;
  const btn = document.querySelector('.sidebar .btn');
  if (isFailureMode) {
    btn.innerHTML = '<i class="fas fa-exclamation-triangle"></i> FAILURE ACTIVE';
    btn.style.background = 'rgba(248, 113, 113, 0.2)';
    btn.style.color = 'var(--accent-danger)';
    alert('Failure injection ENABLED. Payments will now fail to demonstrate Transaction Rollback.');
  } else {
    btn.innerHTML = '<i class="fas fa-bug"></i> Normal Mode';
    btn.style.background = 'linear-gradient(135deg, var(--accent-primary), var(--accent-secondary))';
    btn.style.color = 'white';
    alert('System restored to Normal Mode.');
  }
}

// Dashboard-specific activity log fetch
async function loadRecentOrders() {
  const container = document.getElementById('recent-orders-body');
  if (!container) return;
  
  try {
    const response = await fetch(`${API_URL}/orders/${currentUserId}`);
    const orders = await response.json();
    
    // Show only the latest 4 orders on the dashboard summary
    container.innerHTML = orders.slice(-4).reverse().map(order => `
      <tr>
        <td>${order.orderId}</td>
        <td><span class="${order.status === 'PAID' ? 'status-paid' : 'status-failed'}">${order.status}</span></td>
        <td>${order.paymentMethod}</td>
        <td>₹${order.totalAmount}</td>
        <td>${new Date().toLocaleTimeString()}</td>
      </tr>
    `).join('');
  } catch (error) {
    console.error('Error loading recent orders:', error);
  }
}
