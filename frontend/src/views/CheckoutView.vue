<script setup>
import { ref, onMounted, computed } from 'vue';
import { orderApi, addressApi, voucherApi } from '../services/api';
import { authState } from '../state/authStore';
import { cartState } from '../state/cartStore';

const emit = defineEmits(['navigate', 'order-completed']);

// Saved User Address Book
const savedAddresses = ref([]);
const selectedAddressId = ref(null);

// Shipping Form State (Free edit for this specific order)
const receiverName = ref('');
const receiverPhone = ref('');
const shippingAddress = ref('');
const note = ref('');
const saveToAddressBook = ref(false);
const paymentMethod = ref('COD'); // 'COD', 'BANK_TRANSFER'

const loading = ref(false);
const errorMessage = ref('');

// Voucher state
const voucherCodeInput = ref('');
const appliedVoucher = ref(null);
const voucherMessage = ref('');
const voucherError = ref('');
const voucherLoading = ref(false);
const publicVouchers = ref([]);
const showVoucherModal = ref(false);

async function loadPublicVouchers() {
  try {
    const res = await voucherApi.getActiveVouchers();
    publicVouchers.value = res.data || [];
  } catch (err) {
    console.error('Lỗi tải danh sách voucher công khai:', err);
  }
}

function selectPublicVoucher(v) {
  voucherCodeInput.value = v.code;
  showVoucherModal.value = false;
  handleApplyVoucher();
}

const finalTotalAmount = computed(() => {
  if (appliedVoucher.value && appliedVoucher.value.discountAmount) {
    const total = cartState.totalAmount - appliedVoucher.value.discountAmount;
    return total > 0 ? total : 0;
  }
  return cartState.totalAmount;
});

async function handleApplyVoucher() {
  if (!voucherCodeInput.value.trim()) return;
  voucherLoading.value = true;
  voucherMessage.value = '';
  voucherError.value = '';
  try {
    const res = await voucherApi.validateVoucher(voucherCodeInput.value, cartState.totalAmount);
    if (res.data && res.data.valid) {
      appliedVoucher.value = res.data;
      voucherMessage.value = `Áp dụng thành công mã "${res.data.code}": -${formatPrice(res.data.discountAmount)}`;
    } else {
      appliedVoucher.value = null;
      voucherError.value = res.data?.message || 'Mã giảm giá không hợp lệ!';
    }
  } catch (err) {
    appliedVoucher.value = null;
    voucherError.value = err.message || 'Kiểm tra mã thất bại!';
  } finally {
    voucherLoading.value = false;
  }
}

function handleRemoveVoucher() {
  appliedVoucher.value = null;
  voucherCodeInput.value = '';
  voucherMessage.value = '';
  voucherError.value = '';
}

onMounted(async () => {
  if (!authState.user) {
    emit('navigate', 'login');
    return;
  }

  // Pre-fill shipping info from user profile as default
  receiverName.value = authState.user.fullName || authState.user.username || '';
  receiverPhone.value = authState.user.phone || '';
  shippingAddress.value = authState.user.address || '';

  // Load Saved User Address Book & Public Vouchers
  await Promise.all([
    loadSavedAddresses(),
    loadPublicVouchers()
  ]);

  // Make sure cart data is updated
  await cartState.fetchCart();

  if (!cartState.items.length) {
    emit('navigate', 'cart');
  }
});

async function loadSavedAddresses() {
  try {
    const res = await addressApi.getAddresses(authState.user.id);
    savedAddresses.value = res.data || [];
    
    // Auto-select default address if available
    const defaultAddr = savedAddresses.value.find(a => a.isDefault) || savedAddresses.value[0];
    if (defaultAddr) {
      selectSavedAddress(defaultAddr);
    }
  } catch (err) {
    console.error('Lỗi tải sổ địa chỉ:', err);
  }
}

function selectSavedAddress(addr) {
  selectedAddressId.value = addr.id;
  receiverName.value = addr.recipientName;
  receiverPhone.value = addr.recipientPhone;
  shippingAddress.value = addr.fullAddress || addr.detailAddress;
}

function selectCustomAddress() {
  selectedAddressId.value = 'custom';
  receiverName.value = authState.user.fullName || '';
  receiverPhone.value = authState.user.phone || '';
  shippingAddress.value = '';
}

async function handlePlaceOrder() {
  if (!receiverName.value || !receiverPhone.value || !shippingAddress.value) {
    errorMessage.value = 'Vui lòng điền đầy đủ Họ tên, Số điện thoại và Địa chỉ giao hàng!';
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    // Optionally save custom new address to Address Book if checked
    if (saveToAddressBook.value && selectedAddressId.value === 'custom') {
      try {
        await addressApi.createAddress(authState.user.id, {
          recipientName: receiverName.value,
          recipientPhone: receiverPhone.value,
          detailAddress: shippingAddress.value,
          isDefault: false
        });
      } catch (e) {
        console.warn('Không thể tự động lưu địa chỉ mới:', e);
      }
    }

    const payload = {
      receiverName: receiverName.value,
      receiverPhone: receiverPhone.value,
      shippingAddress: shippingAddress.value,
      note: note.value,
      paymentMethod: paymentMethod.value,
      voucherCode: appliedVoucher.value ? appliedVoucher.value.code : null,
    };

    const res = await orderApi.createOrder(authState.user.id, payload);
    const createdOrder = res.data;

    // Refresh cart state to empty
    await cartState.fetchCart();

    // Emit order completed event to navigate to Order Success page
    emit('order-completed', createdOrder);
  } catch (err) {
    errorMessage.value = err.message || 'Đặt hàng thất bại. Vui lòng kiểm tra lại!';
  } finally {
    loading.value = false;
  }
}

function formatPrice(val) {
  if (!val) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}
</script>

<template>
  <div class="checkout-page">
    <div class="checkout-header">
      <span class="badge-checkout">THANH TOÁN &amp; ĐẶT HÀNG</span>
      <h1>Xác Nhận Đơn Hàng &amp; Thanh Toán</h1>
      <p>Chọn địa chỉ nhận hàng có sẵn hoặc tùy chỉnh cho đơn hàng này</p>
    </div>

    <div v-if="errorMessage" class="alert alert-error">
      ⚠️ {{ errorMessage }}
    </div>

    <div class="checkout-grid">
      <!-- Shipping Form Left -->
      <div class="shipping-section">

        <!-- Saved Address Book Selector -->
        <div class="card-section">
          <div class="section-header-flex">
            <h3>📍 1. Chọn Địa Chỉ Nhận Hàng Có Sẵn</h3>
            <button class="btn-link-profile" @click="emit('navigate', 'profile')">
              ⚙️ Quản lý sổ địa chỉ
            </button>
          </div>
          <p class="section-desc">Chọn từ sổ địa chỉ của bạn hoặc tự chỉnh sửa thông tin giao hàng ở dưới</p>

          <div class="saved-address-list">
            <div
              v-for="addr in savedAddresses"
              :key="addr.id"
              :class="['address-card', selectedAddressId === addr.id ? 'active' : '']"
              @click="selectSavedAddress(addr)"
            >
              <div class="radio-indicator">
                <input type="radio" name="selected_address" :checked="selectedAddressId === addr.id" />
              </div>
              <div class="address-content">
                <div class="address-recipient">
                  <strong>{{ addr.recipientName }}</strong> ({{ addr.recipientPhone }})
                  <span v-if="addr.isDefault" class="badge-default">MẶC ĐỊNH</span>
                </div>
                <div class="address-text">{{ addr.fullAddress || addr.detailAddress }}</div>
              </div>
            </div>

            <div
              :class="['address-card custom-card', selectedAddressId === 'custom' ? 'active' : '']"
              @click="selectCustomAddress"
            >
              <div class="radio-indicator">
                <input type="radio" name="selected_address" :checked="selectedAddressId === 'custom'" />
              </div>
              <div class="address-content">
                <strong>✏️ Tự nhập / Giao đến địa chỉ mới khác</strong>
              </div>
            </div>
          </div>
        </div>

        <!-- Editable Receiver Information for THIS Order -->
        <div class="card-section">
          <h3>📦 Thông Tin Người Nhận Đơn Hàng Này</h3>
          <p class="section-desc">Bạn có thể chỉnh sửa SĐT/Địa chỉ riêng cho đơn hàng này mà không ảnh hưởng tới địa chỉ gốc</p>

          <form class="shipping-form" @submit.prevent>
            <div class="form-row">
              <div class="form-group">
                <label>Họ và Tên người nhận *</label>
                <input v-model="receiverName" type="text" placeholder="Nguyễn Văn A..." required />
              </div>
              <div class="form-group">
                <label>Số điện thoại người nhận *</label>
                <input v-model="receiverPhone" type="text" placeholder="0912345678..." required />
              </div>
            </div>

            <div class="form-group">
              <label>Địa chỉ nhận hàng chi tiết *</label>
              <input v-model="shippingAddress" type="text" placeholder="Số nhà, Tên đường, Phường/Xã, Quận/Huyện, TP..." required />
            </div>

            <div v-if="selectedAddressId === 'custom'" class="checkbox-group">
              <label class="flex-checkbox">
                <input v-model="saveToAddressBook" type="checkbox" />
                <span>Lưu địa chỉ mới này vào Sổ địa chỉ của tôi cho lần mua sau</span>
              </label>
            </div>

            <div class="form-group">
              <label>Ghi chú cho đơn hàng (Tùy chọn)</label>
              <textarea v-model="note" rows="2" placeholder="Giao giờ hành chính, gọi trước khi giao..."></textarea>
            </div>
          </form>
        </div>

        <!-- Payment Method Selector -->
        <div class="card-section">
          <h3>💳 2. Phương Thức Thanh Toán</h3>

          <div class="payment-options">
            <label :class="['payment-card', paymentMethod === 'COD' ? 'active' : '']">
              <input type="radio" value="COD" v-model="paymentMethod" />
              <div class="payment-info">
                <div class="payment-title">💵 Thanh Toán Khi Nhận Hàng (COD)</div>
                <div class="payment-desc">Bạn sẽ thanh toán tiền mặt trực tiếp cho shipper khi nhận sản phẩm.</div>
              </div>
            </label>

            <label :class="['payment-card', paymentMethod === 'BANK_TRANSFER' ? 'active' : '']">
              <input type="radio" value="BANK_TRANSFER" v-model="paymentMethod" />
              <div class="payment-info">
                <div class="payment-title">📲 Chuyển Khoản QR / VNPAY Demo</div>
                <div class="payment-desc">Quét mã QR Ngân hàng (MB, Vietcombank...) hoặc VNPAY để chuyển khoản nhanh.</div>
              </div>
            </label>
          </div>
        </div>
      </div>

      <!-- Order Review & Summary Right -->
      <div class="summary-section">
        <div class="card-section summary-card">
          <h3>🛒 3. Sản Phẩm Xác Nhận Mua</h3>

          <div class="checkout-items-list">
            <div v-for="item in cartState.items" :key="item.id" class="checkout-item-row">
              <img :src="item.thumbnailUrl || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100'" class="item-thumb" />
              <div class="item-info">
                <div class="item-name">{{ item.productName }}</div>
                <div class="item-spec">{{ item.size }} • {{ item.color }} x <strong>{{ item.quantity }}</strong></div>
              </div>
              <div class="item-subtotal">{{ formatPrice(item.subTotal) }}</div>
            </div>
          </div>

          <div class="divider"></div>

          <!-- Voucher Input Section -->
          <div class="voucher-box">
            <div class="voucher-header-row" v-if="publicVouchers.length > 0">
              <span class="voucher-box-title">🎟️ Mã Giảm Giá</span>
              <button class="btn-open-voucher-modal" @click="showVoucherModal = true">
                📋 Chọn mã công khai ({{ publicVouchers.length }})
              </button>
            </div>

            <div class="voucher-input-group">
              <input 
                v-model="voucherCodeInput" 
                type="text" 
                placeholder="Nhập mã bí mật hoặc chọn mã..." 
                class="voucher-input"
                :disabled="!!appliedVoucher"
              />
              <button 
                v-if="!appliedVoucher" 
                class="btn-apply-voucher" 
                @click="handleApplyVoucher"
                :disabled="voucherLoading || !voucherCodeInput.trim()"
              >
                {{ voucherLoading ? '...' : 'Áp dụng' }}
              </button>
              <button 
                v-else 
                class="btn-remove-voucher" 
                @click="handleRemoveVoucher"
              >
                Hủy
              </button>
            </div>
            <div v-if="voucherMessage" class="voucher-msg-success">{{ voucherMessage }}</div>
            <div v-if="voucherError" class="voucher-msg-error">⚠️ {{ voucherError }}</div>
          </div>

          <div class="divider"></div>

          <div class="summary-row">
            <span>Tạm tính:</span>
            <span>{{ formatPrice(cartState.totalAmount) }}</span>
          </div>

          <div class="summary-row" v-if="appliedVoucher">
            <span>Giảm giá (Voucher {{ appliedVoucher.code }}):</span>
            <span class="text-green font-bold">-{{ formatPrice(appliedVoucher.discountAmount) }}</span>
          </div>

          <div class="summary-row">
            <span>Phí vận chuyển:</span>
            <span class="text-green font-bold">MIỄN PHÍ</span>
          </div>

          <div class="divider"></div>

          <div class="total-row">
            <span>Tổng Thanh Toán:</span>
            <span class="total-price">{{ formatPrice(finalTotalAmount) }}</span>
          </div>

          <button
            class="btn-place-order"
            @click="handlePlaceOrder"
            :disabled="loading || !cartState.items.length"
          >
            <span v-if="loading">ĐANG XỬ LÝ ĐƠN HÀNG...</span>
            <span v-else>⚡ XÁC NHẬN ĐẶT HÀNG</span>
          </button>

          <button class="btn-back-cart" @click="emit('navigate', 'cart')">
            ◄ Quay Lại Giỏ Hàng
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Danh Sách Voucher Công Khai -->
    <div v-if="showVoucherModal" class="modal-backdrop" @click.self="showVoucherModal = false">
      <div class="modal-card modal-voucher-card">
        <div class="modal-header">
          <h3>🎟️ Danh Sách Mã Giảm Giá Công Khai</h3>
          <button class="btn-close" @click="showVoucherModal = false">✖</button>
        </div>

        <div class="public-voucher-list">
          <div 
            v-for="v in publicVouchers" 
            :key="v.id" 
            class="public-voucher-card"
          >
            <div class="pvc-left">
              <div class="pvc-header">
                <code class="pvc-code">{{ v.code }}</code>
                <span :class="['type-tag', v.discountType === 'PERCENT' ? 'type-percent' : 'type-fixed']">
                  {{ v.discountType === 'PERCENT' ? 'Giảm %' : 'Giảm Tiền' }}
                </span>
              </div>
              <div class="pvc-name">{{ v.name }}</div>
              <div class="pvc-desc" v-if="v.description">{{ v.description }}</div>
              <div class="pvc-cond">
                <span v-if="v.minOrderAmount > 0">📍 Đơn từ {{ formatPrice(v.minOrderAmount) }}</span>
                <span v-else>📍 Áp dụng mọi đơn hàng</span>
              </div>
            </div>
            <div class="pvc-right">
              <div class="pvc-val">
                {{ v.discountType === 'PERCENT' ? `-${v.discountValue}%` : `-${formatPrice(v.discountValue)}` }}
              </div>
              <button class="btn-select-voucher" @click="selectPublicVoucher(v)">
                Áp Dụng
              </button>
            </div>
          </div>

          <div v-if="publicVouchers.length === 0" class="empty-vouchers">
            Hiện chưa có mã giảm giá công khai nào. Bạn có thể tự nhập mã ẩn/bí mật ở ngoài!
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.checkout-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem;
}

.checkout-header {
  margin-bottom: 2rem;
}

.badge-checkout {
  background: rgba(2, 132, 199, 0.1);
  color: #0284c7;
  font-weight: 800;
  font-size: 0.75rem;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  letter-spacing: 1px;
}

.checkout-header h1 {
  font-size: 2rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0.3rem 0;
}

.checkout-header p {
  color: #64748b;
  font-size: 0.95rem;
}

.checkout-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 2rem;
}

@media (max-width: 900px) {
  .checkout-grid {
    grid-template-columns: 1fr;
  }
}

.shipping-section {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.card-section {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 1.75rem;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
}

.section-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-link-profile {
  background: none;
  border: none;
  color: #0284c7;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
}

.card-section h3 {
  font-size: 1.15rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 0.25rem;
}

.section-desc {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 1.25rem;
}

/* Saved Address Selector List */
.saved-address-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.address-card {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  background: #f8fafc;
  border: 1.5px solid #cbd5e1;
  border-radius: 14px;
  padding: 1rem 1.25rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.address-card.active {
  border-color: #0284c7;
  background: #f0f9ff;
}

.radio-indicator {
  margin-top: 0.2rem;
}

.address-recipient {
  font-size: 0.95rem;
  color: #0f172a;
  margin-bottom: 0.25rem;
}

.badge-default {
  background: #10b981;
  color: white;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.15rem 0.45rem;
  border-radius: 6px;
  margin-left: 0.5rem;
}

.address-text {
  font-size: 0.85rem;
  color: #475569;
}

.custom-card {
  border-style: dashed;
}

.shipping-form {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

@media (max-width: 550px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-group label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #334155;
}

.form-group input, .form-group textarea {
  background: #f8fafc;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  padding: 0.75rem 1rem;
  font-size: 0.9rem;
  color: #0f172a;
  outline: none;
  transition: border-color 0.2s ease;
}

.form-group input:focus, .form-group textarea:focus {
  border-color: #0284c7;
  background: white;
}

.flex-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: #334155;
  cursor: pointer;
}

/* Payment Options */
.payment-options {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1rem;
}

.payment-card {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  background: #f8fafc;
  border: 1.5px solid #cbd5e1;
  border-radius: 14px;
  padding: 1rem 1.25rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.payment-card.active {
  border-color: #0284c7;
  background: #f0f9ff;
}

.payment-card input[type="radio"] {
  margin-top: 0.25rem;
}

.payment-title {
  font-weight: 800;
  font-size: 0.95rem;
  color: #0f172a;
  margin-bottom: 0.2rem;
}

.payment-desc {
  font-size: 0.8rem;
  color: #64748b;
}

/* Summary Card Right */
.summary-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.checkout-items-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-height: 300px;
  overflow-y: auto;
}

.checkout-item-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #f1f5f9;
}

.item-thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: #0f172a;
}

.item-spec {
  font-size: 0.75rem;
  color: #64748b;
}

.item-subtotal {
  font-size: 0.9rem;
  font-weight: 800;
  color: #f97316;
}

.divider {
  height: 1px;
  background: #e2e8f0;
  margin: 0.25rem 0;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
  color: #475569;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-weight: 800;
  color: #0f172a;
}

.total-price {
  font-size: 1.6rem;
  color: #f97316;
}

.btn-place-order {
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: white;
  border: none;
  padding: 1.1rem;
  border-radius: 12px;
  font-weight: 800;
  font-size: 1rem;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(249, 115, 22, 0.3);
  margin-top: 0.5rem;
  transition: transform 0.2s ease;
}

.btn-place-order:hover:not(:disabled) {
  transform: translateY(-2px);
}

.btn-place-order:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-back-cart {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
  padding: 0.75rem;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
}

.alert {
  padding: 0.75rem 1rem;
  border-radius: 10px;
  font-size: 0.85rem;
  margin-bottom: 1.5rem;
}
.alert-error { background: #fef2f2; border: 1px solid #fecaca; color: #dc2626; }

/* Voucher Box */
.voucher-box {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.voucher-input-group {
  display: flex;
  gap: 0.5rem;
}

.voucher-input {
  flex: 1;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0.5rem 0.75rem;
  font-size: 0.85rem;
  font-weight: 700;
  outline: none;
  text-transform: uppercase;
}

.voucher-input:focus {
  border-color: #6366f1;
}

.btn-apply-voucher {
  background: #6366f1;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 0.5rem 1rem;
  font-weight: 800;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-apply-voucher:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-remove-voucher {
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 0.5rem 1rem;
  font-weight: 800;
  font-size: 0.85rem;
  cursor: pointer;
}

.voucher-msg-success {
  font-size: 0.8rem;
  font-weight: 700;
  color: #16a34a;
}

.voucher-msg-error {
  font-size: 0.8rem;
  font-weight: 700;
  color: #dc2626;
}

/* Public Voucher Picker Header & Modal */
.voucher-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.25rem;
}

.voucher-box-title {
  font-size: 0.85rem;
  font-weight: 800;
  color: #0f172a;
}

.btn-open-voucher-modal {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
  padding: 0.25rem 0.5rem;
  font-size: 0.78rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-open-voucher-modal:hover {
  background: #dcfce7;
}

.modal-backdrop {
  position: fixed;
  top: 0; left: 0; width: 100vw; height: 100vh;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex; justify-content: center; align-items: center;
  z-index: 1000; padding: 1rem;
}

.modal-voucher-card {
  background: white;
  border-radius: 20px;
  width: 100%;
  max-width: 550px;
  padding: 1.5rem;
  box-shadow: 0 10px 40px rgba(0,0,0,0.15);
}

.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.modal-header h3 { font-size: 1.15rem; font-weight: 800; color: #0f172a; margin: 0; }
.btn-close { background: none; border: none; font-size: 1.2rem; cursor: pointer; }

.public-voucher-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 0.25rem;
}

.public-voucher-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8fafc;
  border: 1.5px dashed #cbd5e1;
  border-radius: 12px;
  padding: 0.9rem;
  transition: border-color 0.15s ease;
}

.public-voucher-card:hover {
  border-color: #6366f1;
  background: #f5f3ff;
}

.pvc-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.pvc-code {
  background: #eef2ff;
  color: #4f46e5;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  font-weight: 800;
  font-family: monospace;
  font-size: 0.85rem;
}

.type-tag {
  font-size: 0.7rem;
  font-weight: 800;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
}
.type-percent { background: #fef3c7; color: #d97706; }
.type-fixed { background: #e0f2fe; color: #0369a1; }

.pvc-name {
  font-weight: 700;
  color: #0f172a;
  font-size: 0.9rem;
}

.pvc-desc {
  font-size: 0.78rem;
  color: #64748b;
  margin-top: 0.1rem;
}

.pvc-cond {
  font-size: 0.72rem;
  color: #0284c7;
  font-weight: 700;
  margin-top: 0.25rem;
}

.pvc-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.4rem;
}

.pvc-val {
  font-weight: 900;
  color: #16a34a;
  font-size: 1.1rem;
}

.btn-select-voucher {
  background: #6366f1;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 0.4rem 0.8rem;
  font-size: 0.8rem;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.15s;
}

.btn-select-voucher:hover {
  transform: scale(1.05);
}

.empty-vouchers {
  text-align: center;
  padding: 2rem;
  color: #64748b;
  font-size: 0.85rem;
}
</style>

<!-- Feature Implementation: ui review đơn, chọn thanh toán -->

<!-- Feature Implementation: ui form địa chỉ, autocomplete -->
