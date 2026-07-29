<script setup>
import { onMounted } from 'vue';
import { cartState } from '../state/cartStore';
import { authState } from '../state/authStore';

const emit = defineEmits(['navigate']);

onMounted(async () => {
  if (!authState.user) {
    emit('navigate', 'login');
    return;
  }
  await cartState.fetchCart();
});

async function handleUpdateQty(item, delta) {
  const newQty = item.quantity + delta;
  if (newQty <= 0) {
    if (confirm(`Bạn có chắc chắn muốn xóa "${item.productName}" khỏi giỏ hàng?`)) {
      await cartState.removeItem(item.id);
    }
  } else {
    await cartState.updateQuantity(item.id, newQty);
  }
}

async function handleRemove(item) {
  if (confirm(`Xóa sản phẩm "${item.productName}" (${item.size} - ${item.color}) khỏi giỏ?`)) {
    await cartState.removeItem(item.id);
  }
}

async function handleClearCart() {
  if (confirm('Bạn có chắc muốn dọn sạch toàn bộ giỏ hàng?')) {
    await cartState.clearCart();
  }
}

function formatPrice(val) {
  if (!val) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}
</script>

<template>
  <div class="cart-page">
    <div class="cart-header">
      <div>
        <span class="badge-cart">GIỎ HÀNG THỜI TRANG</span>
        <h1>Giỏ Hàng Của Bạn</h1>
        <p>Quản lý các sản phẩm đã chọn, điều chỉnh số lượng và tính tổng tiền</p>
      </div>

      <button v-if="cartState.items.length" class="btn-clear-cart" @click="handleClearCart">
        🗑️ Dọn Sạch Giỏ Hàng
      </button>
    </div>

    <!-- Empty Cart State -->
    <div v-if="!cartState.items.length" class="empty-cart-card">
      <div class="empty-icon">🛍️</div>
      <h2>Giỏ hàng của bạn đang trống!</h2>
      <p>Hãy chọn cho mình những bộ trang phục thời trang thanh lịch nhất từ FS Shop.</p>
      <button class="btn-shop-now" @click="emit('navigate', 'products')">
        KHÁM PHÁ SẢN PHẨM NGAY ➔
      </button>
    </div>

    <!-- Cart Content Grid -->
    <div v-else class="cart-grid">
      <!-- Items List Left -->
      <div class="cart-items-section">
        <div v-for="item in cartState.items" :key="item.id" class="cart-item-card">
          <img :src="item.thumbnailUrl || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=200'" class="item-img" />

          <div class="item-details">
            <h3 class="item-title">{{ item.productName }}</h3>
            <div class="item-meta">
              <span class="meta-tag">Size: <strong>{{ item.size }}</strong></span>
              <span class="meta-tag">Màu: <strong>{{ item.color }}</strong></span>
              <span class="sku-tag">SKU: <code>{{ item.productSku }}</code></span>
            </div>

            <div class="item-price">
              <span class="unit-price">{{ formatPrice(item.unitPrice) }}</span>
              <span v-if="item.price && item.price > item.unitPrice" class="old-price">
                {{ formatPrice(item.price) }}
              </span>
            </div>
          </div>

          <!-- Quantity Picker (US14) -->
          <div class="qty-section">
            <div class="quantity-picker">
              <button class="btn-qty" @click="handleUpdateQty(item, -1)">-</button>
              <span class="qty-val">{{ item.quantity }}</span>
              <button class="btn-qty" @click="handleUpdateQty(item, 1)" :disabled="item.quantity >= item.availableStock">+</button>
            </div>
            <span class="stock-info">Tồn kho: {{ item.availableStock }} SP</span>
          </div>

          <!-- Subtotal & Remove Button (US15) -->
          <div class="subtotal-section">
            <span class="subtotal-val">{{ formatPrice(item.subTotal) }}</span>
            <button class="btn-remove-item" @click="handleRemove(item)" title="Xóa khỏi giỏ">
              🗑️ Xóa
            </button>
          </div>
        </div>
      </div>

      <!-- Order Summary Card Right -->
      <div class="summary-section">
        <div class="summary-card">
          <h3 class="summary-title">TỔNG ĐƠN HÀNG</h3>

          <div class="summary-row">
            <span>Tổng số lượng sản phẩm:</span>
            <strong class="font-bold">{{ cartState.totalItems }} SP</strong>
          </div>

          <div class="summary-row">
            <span>Tạm tính:</span>
            <span>{{ formatPrice(cartState.totalAmount) }}</span>
          </div>

          <div class="summary-row">
            <span>Phí vận chuyển:</span>
            <span class="text-green font-bold">MIỄN PHÍ</span>
          </div>

          <div class="divider"></div>

          <div class="summary-total-row">
            <span>Tổng Thành Tiền:</span>
            <span class="total-price">{{ formatPrice(cartState.totalAmount) }}</span>
          </div>

          <button class="btn-checkout" @click="emit('navigate', 'checkout')">
            TIẾN HÀNH ĐẶT HÀNG ➔
          </button>

          <button class="btn-continue-shop" @click="emit('navigate', 'products')">
            ◄ Tiếp Tục Mua Sắm
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.badge-cart {
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
  font-weight: 800;
  font-size: 0.75rem;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  letter-spacing: 1px;
}

.cart-header h1 {
  font-size: 2rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0.3rem 0;
}

.cart-header p {
  color: #64748b;
  font-size: 0.95rem;
}

.btn-clear-cart {
  background: #fff1f2;
  border: 1px solid #fecdd3;
  color: #e11d48;
  padding: 0.6rem 1.25rem;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  font-size: 0.85rem;
}

/* Empty State */
.empty-cart-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 4rem 2rem;
  text-align: center;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-cart-card h2 {
  font-size: 1.5rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 0.5rem;
}

.empty-cart-card p {
  color: #64748b;
  margin-bottom: 2rem;
}

.btn-shop-now {
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: white;
  border: none;
  padding: 0.9rem 2rem;
  border-radius: 12px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(249, 115, 22, 0.25);
}

/* Cart Grid Layout */
.cart-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 2rem;
}

@media (max-width: 900px) {
  .cart-grid {
    grid-template-columns: 1fr;
  }
}

.cart-items-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.cart-item-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 1.25rem;
  display: grid;
  grid-template-columns: 90px 1fr 130px 120px;
  align-items: center;
  gap: 1.25rem;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
}

@media (max-width: 650px) {
  .cart-item-card {
    grid-template-columns: 80px 1fr;
    gap: 1rem;
  }
}

.item-img {
  width: 90px;
  height: 90px;
  border-radius: 12px;
  object-fit: cover;
  background: #f8fafc;
}

.item-title {
  font-size: 1.05rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 0.4rem;
}

.item-meta {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
  margin-bottom: 0.5rem;
}

.meta-tag {
  background: #f1f5f9;
  color: #334155;
  font-size: 0.75rem;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
}

.sku-tag {
  font-size: 0.75rem;
  color: #94a3b8;
}

.item-price {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.unit-price {
  font-weight: 800;
  color: #f97316;
  font-size: 1rem;
}

.old-price {
  font-size: 0.8rem;
  color: #94a3b8;
  text-decoration: line-through;
}

.qty-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
}

.quantity-picker {
  display: flex;
  align-items: center;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  overflow: hidden;
  background: #f8fafc;
}

.btn-qty {
  background: none;
  border: none;
  width: 32px;
  height: 32px;
  font-weight: 700;
  cursor: pointer;
}

.qty-val {
  padding: 0 0.6rem;
  font-weight: 800;
  font-size: 0.9rem;
}

.stock-info {
  font-size: 0.7rem;
  color: #64748b;
}

.subtotal-section {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.5rem;
}

.subtotal-val {
  font-size: 1.1rem;
  font-weight: 800;
  color: #0f172a;
}

.btn-remove-item {
  background: none;
  border: none;
  color: #ef4444;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-remove-item:hover {
  text-decoration: underline;
}

/* Summary Card Right */
.summary-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 1.75rem;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.summary-title {
  font-size: 1.1rem;
  font-weight: 800;
  color: #0f172a;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #f1f5f9;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
  color: #475569;
}

.divider {
  height: 1px;
  background: #e2e8f0;
  margin: 0.25rem 0;
}

.summary-total-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-weight: 800;
  color: #0f172a;
}

.total-price {
  font-size: 1.5rem;
  color: #f97316;
}

.btn-checkout {
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: white;
  border: none;
  padding: 1rem;
  border-radius: 12px;
  font-weight: 800;
  font-size: 0.95rem;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(249, 115, 22, 0.25);
  margin-top: 0.5rem;
}

.btn-continue-shop {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
  padding: 0.75rem;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
}
</style>

<!-- Feature Implementation: ui nút (+/-), tính tổng tiền -->
