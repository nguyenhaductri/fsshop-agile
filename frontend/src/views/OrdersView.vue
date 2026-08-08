<script setup>
import { ref, onMounted, computed } from 'vue';
import { orderApi, reviewApi, publicProductApi } from '../services/api';
import { authState } from '../state/authStore';
import InvoiceModal from '../components/InvoiceModal.vue';

const emit = defineEmits(['navigate']);

const orders = ref([]);
const loading = ref(false);
const activeTab = ref('ALL'); // 'ALL', 'PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED'

// Invoice Modal state
const showInvoiceModal = ref(false);
const invoiceTargetOrder = ref(null);

function openInvoiceModal(order) {
  invoiceTargetOrder.value = order;
  showInvoiceModal.value = true;
}

// Detail Modal state
const selectedOrder = ref(null);
const showDetailModal = ref(false);

// Review Modal state
const showReviewModal = ref(false);
const reviewTargetItem = ref(null);
const reviewTargetOrder = ref(null);
const reviewStars = ref(5);
const reviewComment = ref('');
const reviewLoading = ref(false);

// Cancel Order Modal state
const showCancelModal = ref(false);
const cancelingOrderId = ref(null);
const cancelReason = ref('Thay đổi ý định mua hàng');
const cancelingLoading = ref(false);

// User Reviews State to hide reviewed buttons
const userReviews = ref([]);

onMounted(async () => {
  if (!authState.user) {
    emit('navigate', 'login');
    return;
  }
  await Promise.all([fetchOrders(), fetchUserReviews()]);
});

async function fetchOrders() {
  loading.value = true;
  try {
    const res = await orderApi.getOrders(authState.user.id);
    orders.value = res.data || [];
  } catch (err) {
    console.error('Lỗi tải danh sách đơn hàng:', err);
  } finally {
    loading.value = false;
  }
}

async function fetchUserReviews() {
  try {
    const res = await reviewApi.getUserReviews(authState.user.id);
    userReviews.value = res.data || [];
  } catch (err) {
    console.error('Lỗi tải danh sách đánh giá của tôi:', err);
  }
}

function isItemReviewed(orderId, productId) {
  if (!orderId || !productId || !userReviews.value.length) return false;
  return userReviews.value.some(r => r.orderId === orderId && r.productId === productId);
}

const filteredOrders = computed(() => {
  if (activeTab.value === 'ALL') return orders.value;
  return orders.value.filter(o => o.orderStatus === activeTab.value);
});

async function goToProduct(itemOrId) {
  let targetId = null;
  if (typeof itemOrId === 'number' || typeof itemOrId === 'string') {
    targetId = itemOrId;
  } else if (itemOrId && typeof itemOrId === 'object') {
    targetId = itemOrId.productId || itemOrId.id;
    if (!targetId && (itemOrId.productName || itemOrId.productSku)) {
      try {
        const res = await publicProductApi.getProducts({ keyword: itemOrId.productSku || itemOrId.productName });
        if (res.data && res.data.length > 0) {
          targetId = res.data[0].id;
        }
      } catch (err) {
        console.error('Lỗi tìm sản phẩm:', err);
      }
    }
  }

  showDetailModal.value = false;
  if (targetId) {
    emit('navigate', 'product-detail', targetId);
  } else {
    emit('navigate', 'products');
  }
}

function openDetailModal(order) {
  selectedOrder.value = order;
  showDetailModal.value = true;
}

function openCancelModal(orderId) {
  cancelingOrderId.value = orderId;
  cancelReason.value = 'Thay đổi ý định mua hàng';
  showCancelModal.value = true;
}

async function handleCancelOrder() {
  if (!cancelingOrderId.value) return;

  cancelingLoading.value = true;
  try {
    await orderApi.cancelOrder(authState.user.id, cancelingOrderId.value, cancelReason.value);
    showCancelModal.value = false;
    showDetailModal.value = false;
    await fetchOrders();
    alert('Đã hủy đơn hàng thành công và tự động hoàn lại tồn kho!');
  } catch (err) {
    alert(err.message || 'Không thể hủy đơn hàng!');
  } finally {
    cancelingLoading.value = false;
  }
}

async function handleConfirmReceived(orderId) {
  if (confirm('Bạn xác nhận đã nhận được đầy đủ sản phẩm và muốn hoàn tất đơn hàng?')) {
    try {
      await orderApi.confirmReceived(authState.user.id, orderId);
      showDetailModal.value = false;
      await fetchOrders();
      alert('Cảm ơn bạn! Đơn hàng đã được xác nhận hoàn tất thành công 2 phía.');
    } catch (err) {
      alert(err.message || 'Xác nhận thất bại!');
    }
  }
}

function openReviewModal(order, item) {
  reviewTargetOrder.value = order;
  reviewTargetItem.value = item;
  reviewStars.value = 5;
  reviewComment.value = '';
  showReviewModal.value = true;
}

async function handleSaveReview() {
  if (!reviewTargetItem.value || !reviewTargetOrder.value) return;

  reviewLoading.value = true;
  try {
    await reviewApi.createReview(authState.user.id, {
      productId: reviewTargetItem.value.productId,
      orderId: reviewTargetOrder.value.id,
      ratingStars: reviewStars.value,
      comment: reviewComment.value,
    });
    showReviewModal.value = false;
    await fetchUserReviews();
    alert('Cảm ơn bạn! Đã gửi đánh giá sản phẩm thành công.');
  } catch (err) {
    alert(err.message || 'Gửi đánh giá thất bại!');
  } finally {
    reviewLoading.value = false;
  }
}

function getStatusBadgeClass(status) {
  switch (status) {
    case 'PENDING': return 'status-pending';
    case 'CONFIRMED': return 'status-confirmed';
    case 'SHIPPING': return 'status-shipping';
    case 'DELIVERED': return 'status-delivered';
    case 'COMPLETED': return 'status-completed';
    case 'CANCELLED': return 'status-cancelled';
    case 'CANCEL_REQUESTED': return 'status-cancel-req';
    default: return '';
  }
}

function getStatusText(status) {
  switch (status) {
    case 'PENDING': return '⏳ Chờ Cửa Hàng Duyệt';
    case 'CONFIRMED': return '✅ Đã Duyệt Đơn';
    case 'SHIPPING': return '🚚 Đang Giao Hàng';
    case 'DELIVERED': return '📦 Đã Giao Hàng (Xác nhận 1 phía từ Admin)';
    case 'COMPLETED': return '🎉 Hoàn Thành (Đã xác nhận 2 phía)';
    case 'CANCELLED': return '❌ Đã Hủy';
    case 'CANCEL_REQUESTED': return '⏳ Chờ Admin Duyệt Hủy';
    default: return status;
  }
}

function formatPrice(val) {
  if (!val) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleString('vi-VN');
}
</script>

<template>
  <div class="orders-page">
    <div class="page-header">
      <div>
        <span class="badge-orders">THEO DÕI ĐƠN HÀNG</span>
        <h1>Lịch Sử Đơn Hàng Của Bạn</h1>
        <p>Theo dõi tiến độ giao hàng, chi tiết đơn và lịch sử cập nhật trạng thái</p>
      </div>
    </div>

    <!-- Status Tabs (US18) -->
    <div class="status-tabs">
      <button :class="['tab-btn', activeTab === 'ALL' ? 'active' : '']" @click="activeTab = 'ALL'">
        Tất Cả ({{ orders.length }})
      </button>
      <button :class="['tab-btn', activeTab === 'PENDING' ? 'active' : '']" @click="activeTab = 'PENDING'">
        ⏳ Chờ Xác Nhận
      </button>
      <button :class="['tab-btn', activeTab === 'CONFIRMED' ? 'active' : '']" @click="activeTab = 'CONFIRMED'">
        ✅ Đã Xác Nhận
      </button>
      <button :class="['tab-btn', activeTab === 'SHIPPING' ? 'active' : '']" @click="activeTab = 'SHIPPING'">
        🚚 Đang Giao
      </button>
      <button :class="['tab-btn', activeTab === 'COMPLETED' ? 'active' : '']" @click="activeTab = 'COMPLETED'">
        🎉 Hoàn Thành
      </button>
      <button :class="['tab-btn', activeTab === 'CANCELLED' ? 'active' : '']" @click="activeTab = 'CANCELLED'">
        ❌ Đã Hủy
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      ⏳ Đang tải thông tin đơn hàng...
    </div>

    <!-- Empty State -->
    <div v-else-if="!filteredOrders.length" class="empty-orders">
      <div class="empty-icon">📦</div>
      <h3>Chưa có đơn hàng nào ở trạng thái này</h3>
      <p>Hãy chọn những bộ trang phục thời trang mới nhất từ FS SHOP nhé.</p>
      <button class="btn-shop" @click="emit('navigate', 'products')">KHÁM PHÁ SẢN PHẨM ➔</button>
    </div>

    <!-- Orders List Grid (US18) -->
    <div v-else class="orders-list">
      <div v-for="order in filteredOrders" :key="order.id" class="order-card">
        <div class="order-card-header">
          <div class="order-code">
            <span>Mã Đơn:</span>
            <strong>{{ order.orderCode }}</strong>
            <span class="order-date">• {{ formatDate(order.createdAt) }}</span>
          </div>
          <div :class="['status-badge', getStatusBadgeClass(order.orderStatus)]">
            {{ getStatusText(order.orderStatus) }}
          </div>
        </div>

        <div class="order-card-body">
          <div v-for="item in order.items" :key="item.id" class="order-item-row">
            <img 
              :src="item.thumbnailUrl || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100'" 
              class="item-img clickable" 
              @click="goToProduct(item)"
              title="Xem chi tiết sản phẩm"
            />
            <div class="item-info">
              <h4 class="item-title clickable" @click="goToProduct(item)" title="Xem chi tiết sản phẩm">{{ item.productName }}</h4>
              <div class="item-meta">Size: {{ item.size }} • Màu: {{ item.color }} • Số lượng: <strong>x{{ item.quantity }}</strong></div>
            </div>
            <div class="item-actions-col">
              <div class="item-price">{{ formatPrice(item.subTotal) }}</div>
              <template v-if="order.orderStatus === 'COMPLETED'">
                <button
                  v-if="!isItemReviewed(order.id, item.productId)"
                  class="btn-review-item"
                  @click="openReviewModal(order, item)"
                >
                  ⭐ Đánh Giá
                </button>
                <span v-else class="badge-reviewed-item">
                  ✅ Đã Đánh Giá
                </span>
              </template>
            </div>
          </div>
        </div>

        <div class="order-card-footer">
          <div class="footer-info">
            <span>Tổng số tiền ({{ order.items ? order.items.length : 0 }} SP):</span>
            <strong class="total-price">{{ formatPrice(order.totalAmount) }}</strong>
          </div>

          <div class="footer-actions">
            <!-- Free Cancel Button before Admin approval (PENDING) -->
            <button
              v-if="order.orderStatus === 'PENDING'"
              class="btn-cancel-order"
              @click="openCancelModal(order.id)"
            >
              🚫 HỦY ĐƠN HÀNG
            </button>

            <!-- Request Cancel Button after Admin approval (CONFIRMED/SHIPPING) -->
            <button
              v-else-if="order.orderStatus === 'CONFIRMED' || order.orderStatus === 'SHIPPING'"
              class="btn-cancel-order"
              @click="openCancelModal(order.id)"
            >
              ⚠️ GỬI YÊU CẦU HỦY ĐƠN
            </button>

            <span v-else-if="order.orderStatus === 'CANCEL_REQUESTED'" class="badge-waiting-cancel">
              ⏳ ĐANG CHỜ ADMIN DUYỆT YÊU CẦU HỦY
            </span>

            <!-- Customer 2-Way Receipt Confirmation (ONLY VISIBLE WHEN ADMIN MARKED DELIVERED!) -->
            <button
              v-if="order.orderStatus === 'DELIVERED'"
              class="btn-confirm-receipt"
              @click="handleConfirmReceived(order.id)"
            >
              🎉 XÁC NHẬN ĐÃ NHẬN HÀNG (HOÀN THÀNH 2 PHÍA)
            </button>

            <button 
              v-if="['CONFIRMED', 'DELIVERED', 'COMPLETED'].includes(order.orderStatus)" 
              class="btn-invoice-order" 
              @click="openInvoiceModal(order)"
            >
              🧾 IN HÓA ĐƠN
            </button>

            <button class="btn-detail-order" @click="openDetailModal(order)">
              📋 XEM CHI TIẾT &amp; TIMELINE ➔
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Detail & Timeline Modal (US19 & US21) -->
    <div v-if="showDetailModal && selectedOrder" class="modal-backdrop" @click.self="showDetailModal = false">
      <div class="modal-card modal-lg">
        <div class="modal-header">
          <div>
            <h3>📋 Chi Tiết Đơn Hàng: {{ selectedOrder.orderCode }}</h3>
            <span :class="['status-badge', getStatusBadgeClass(selectedOrder.orderStatus)]">
              {{ getStatusText(selectedOrder.orderStatus) }}
            </span>
          </div>
          <button class="btn-close" @click="showDetailModal = false">✖</button>
        </div>

        <div class="modal-body-scroll">
          <!-- Recipient & Delivery Info -->
          <div class="info-section">
            <h4>📦 Thông Tin Nhận Hàng</h4>
            <div class="info-grid">
              <div><span>Người nhận:</span> <strong>{{ selectedOrder.receiverName }}</strong></div>
              <div><span>Số điện thoại:</span> <strong>{{ selectedOrder.receiverPhone }}</strong></div>
              <div><span>Địa chỉ giao:</span> <span>{{ selectedOrder.shippingAddress }}</span></div>
              <div><span>Phương thức thanh toán:</span> <strong>{{ selectedOrder.paymentMethod === 'COD' ? '💵 Thanh toán khi nhận hàng (COD)' : '📲 Chuyển khoản QR / VNPAY' }}</strong></div>
              <div v-if="selectedOrder.note"><span>Ghi chú:</span> <em>{{ selectedOrder.note }}</em></div>
            </div>
          </div>

          <!-- Items List -->
          <div class="info-section">
            <h4>🛒 Sản Phẩm Đã Mua</h4>
            <div class="items-table">
              <div v-for="item in selectedOrder.items" :key="item.id" class="table-item-row">
                <img 
                  :src="item.thumbnailUrl || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100'" 
                  class="table-thumb clickable" 
                  @click="goToProduct(item)"
                  title="Xem chi tiết sản phẩm"
                />
                <div class="table-info">
                  <div class="table-name clickable" @click="goToProduct(item)" title="Xem chi tiết sản phẩm">{{ item.productName }}</div>
                  <div class="table-spec">Size: {{ item.size }} | Màu: {{ item.color }}</div>
                </div>
                <div class="table-qty">x{{ item.quantity }}</div>
                <div class="table-price-col">
                  <div class="table-price">{{ formatPrice(item.subTotal) }}</div>
                  <template v-if="selectedOrder.orderStatus === 'COMPLETED'">
                    <button
                      v-if="!isItemReviewed(selectedOrder.id, item.productId)"
                      class="btn-review-item-sm"
                      @click="openReviewModal(selectedOrder, item)"
                    >
                      ⭐ Đánh Giá
                    </button>
                    <span v-else class="badge-reviewed-item-sm">
                      ✅ Đã Đánh Giá
                    </span>
                  </template>
                </div>
              </div>
            </div>
          </div>

          <!-- Order Status Timeline History (US21) -->
          <div class="info-section">
            <h4>⏱️ Lịch Sử Tiến Độ Đơn Hàng (Timeline)</h4>
            <div v-if="selectedOrder.histories && selectedOrder.histories.length" class="timeline-list">
              <div v-for="h in selectedOrder.histories" :key="h.id" class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                  <div class="timeline-title">
                    <strong>{{ getStatusText(h.status) }}</strong>
                    <span class="timeline-time">{{ formatDate(h.createdAt) }}</span>
                  </div>
                  <div class="timeline-desc">{{ h.description }}</div>
                </div>
              </div>
            </div>
            <div v-else class="text-gray">Chưa có bản ghi lịch sử tiến độ.</div>
          </div>
        </div>

        <div class="modal-footer">
          <button 
            v-if="['CONFIRMED', 'DELIVERED', 'COMPLETED'].includes(selectedOrder.orderStatus)" 
            class="btn-invoice-order" 
            @click="openInvoiceModal(selectedOrder)"
          >
            🧾 IN HÓA ĐƠN (PDF)
          </button>
          <button
            v-if="selectedOrder.orderStatus === 'PENDING'"
            class="btn-cancel-order"
            @click="openCancelModal(selectedOrder.id)"
          >
            🚫 HỦY ĐƠN HÀNG
          </button>
          <button class="btn-close-modal" @click="showDetailModal = false">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Printable Invoice Modal Overlay -->
    <InvoiceModal
      v-if="showInvoiceModal && invoiceTargetOrder"
      :order="invoiceTargetOrder"
      @close="showInvoiceModal = false"
    />

    <!-- Cancel Order Confirmation Modal (US20) -->
    <div v-if="showCancelModal" class="modal-backdrop" @click.self="showCancelModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>🚫 Hủy Đơn Hàng</h3>
          <button class="btn-close" @click="showCancelModal = false">✖</button>
        </div>

        <div class="modal-body">
          <p>Bạn có chắc chắn muốn hủy đơn hàng này không? Tồn kho sản phẩm sẽ được tự động hoàn trả lại.</p>

          <div class="form-group margin-top">
            <label>Lý do hủy đơn *</label>
            <select v-model="cancelReason" class="select-reason">
              <option value="Thay đổi ý định mua hàng">Thay đổi ý định mua hàng</option>
              <option value="Muốn đổi sang sản phẩm / size khác">Muốn đổi sang sản phẩm / size khác</option>
              <option value="Đặt trùng đơn hàng">Đặt trùng đơn hàng</option>
              <option value="Cập nhật lại địa chỉ giao hàng">Cập nhật lại địa chỉ giao hàng</option>
              <option value="Lý do khác">Lý do khác</option>
            </select>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel-modal" @click="showCancelModal = false">Quay Lại</button>
          <button class="btn-confirm-cancel" @click="handleCancelOrder" :disabled="cancelingLoading">
            <span v-if="cancelingLoading">ĐANG HỦY...</span>
            <span v-else>XÁC NHẬN HỦY ĐƠN</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Product Review Modal (Sprint 8) -->
    <div v-if="showReviewModal && reviewTargetItem" class="modal-backdrop" @click.self="showReviewModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>⭐ Đánh Giá Sản Phẩm</h3>
          <button class="btn-close" @click="showReviewModal = false">✖</button>
        </div>

        <div class="modal-body">
          <div class="review-product-info">
            <img :src="reviewTargetItem.thumbnailUrl || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100'" class="review-thumb" />
            <div>
              <h4 class="review-prod-title">{{ reviewTargetItem.productName }}</h4>
              <div class="text-sm text-gray">Phân loại: Size {{ reviewTargetItem.size }} | Màu {{ reviewTargetItem.color }}</div>
            </div>
          </div>

          <div class="form-group margin-top">
            <label class="star-rating-label">Chấm điểm chất lượng *</label>
            <div class="star-picker">
              <span
                v-for="star in 5"
                :key="star"
                :class="['star-icon', star <= reviewStars ? 'active' : '']"
                @click="reviewStars = star"
              >
                ★
              </span>
              <span class="star-text-hint">({{ reviewStars }} / 5 Sao)</span>
            </div>
          </div>

          <div class="form-group margin-top">
            <label>Nội dung nhận xét &amp; đánh giá (Không bắt buộc)</label>
            <textarea
              v-model="reviewComment"
              rows="4"
              placeholder="Nhập nội dung nhận xét của bạn (tùy chọn)..."
              class="review-textarea"
            ></textarea>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel-modal" @click="showReviewModal = false">Hủy Bỏ</button>
          <button class="btn-confirm-review" @click="handleSaveReview" :disabled="reviewLoading">
            <span v-if="reviewLoading">ĐANG GỬI...</span>
            <span v-else>GỬI ĐÁNH GIÁ</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.orders-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem;
}

.page-header {
  margin-bottom: 2rem;
}

.badge-orders {
  background: rgba(2, 132, 199, 0.1);
  color: #0284c7;
  font-weight: 800;
  font-size: 0.75rem;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  letter-spacing: 1px;
}

.page-header h1 {
  font-size: 2rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0.3rem 0;
}

.page-header p {
  color: #64748b;
  font-size: 0.95rem;
}

/* Status Tabs */
.status-tabs {
  display: flex;
  gap: 0.6rem;
  overflow-x: auto;
  padding-bottom: 0.5rem;
  margin-bottom: 2rem;
  border-bottom: 1px solid #e2e8f0;
}

.tab-btn {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  color: #475569;
  padding: 0.6rem 1.1rem;
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.tab-btn.active {
  background: #0284c7;
  color: white;
  border-color: #0284c7;
  box-shadow: 0 4px 12px rgba(2, 132, 199, 0.25);
}

/* Loading & Empty */
.loading-state {
  text-align: center;
  padding: 3rem;
  color: #64748b;
  font-weight: 600;
}

.empty-orders {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 4rem 2rem;
  text-align: center;
}

.empty-icon {
  font-size: 3.5rem;
  margin-bottom: 0.5rem;
}

.empty-orders h3 {
  font-size: 1.3rem;
  font-weight: 800;
  color: #0f172a;
}

.empty-orders p {
  color: #64748b;
  margin-bottom: 1.5rem;
}

.btn-shop {
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: white;
  border: none;
  padding: 0.85rem 1.75rem;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
}

/* Orders List */
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.order-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
}

.order-card-header {
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  padding: 1rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-code {
  font-size: 0.95rem;
  color: #334155;
}

.order-date {
  color: #94a3b8;
  font-size: 0.8rem;
  margin-left: 0.4rem;
}

/* Status Badges */
.status-badge {
  padding: 0.3rem 0.75rem;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 800;
}
.status-pending { background: #fef3c7; color: #d97706; }
.status-confirmed { background: #e0f2fe; color: #0284c7; }
.status-shipping { background: #e0e7ff; color: #4338ca; }
.status-delivered { background: #dbeafe; color: #1d4ed8; }
.status-completed { background: #dcfce7; color: #15803d; }
.status-cancelled { background: #ffe4e6; color: #e11d48; }
.status-cancel-req { background: #fef2f2; color: #dc2626; border: 1px dashed #ef4444; }

.badge-waiting-cancel {
  background: #fef2f2;
  color: #dc2626;
  font-size: 0.8rem;
  font-weight: 800;
  padding: 0.4rem 0.8rem;
  border-radius: 10px;
  border: 1px dashed #fca5a5;
}

.order-card-body {
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.order-item-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.item-img {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  object-fit: cover;
}

.item-info {
  flex: 1;
}

.clickable {
  cursor: pointer;
  transition: all 0.15s ease;
}

.item-title.clickable:hover,
.table-name.clickable:hover {
  color: #0284c7 !important;
  font-weight: 800 !important;
  text-decoration: underline;
}

.item-img.clickable:hover,
.table-thumb.clickable:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(2, 132, 199, 0.2);
}

.item-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 0.2rem;
}

.item-meta {
  font-size: 0.8rem;
  color: #64748b;
}

.item-price {
  font-weight: 800;
  color: #0f172a;
  font-size: 0.95rem;
}

.order-card-footer {
  border-top: 1px solid #f1f5f9;
  padding: 1rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.total-price {
  font-size: 1.25rem;
  color: #f97316;
  margin-left: 0.4rem;
}

.footer-actions {
  display: flex;
  gap: 0.75rem;
}

.btn-cancel-order {
  background: #fff1f2;
  border: 1px solid #fecdd3;
  color: #e11d48;
  padding: 0.6rem 1rem;
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-confirm-receipt {
  background: #f0fdf4;
  border: 1.5px solid #16a34a;
  color: #16a34a;
  padding: 0.6rem 1rem;
  border-radius: 10px;
  font-weight: 800;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-detail-order {
  background: #f0f9ff;
  border: 1.5px solid #0284c7;
  color: #0284c7;
  padding: 0.6rem 1.2rem;
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-invoice-order {
  background: #f1f5f9;
  color: #0f172a;
  border: 1px solid #cbd5e1;
  padding: 0.6rem 1.1rem;
  border-radius: 10px;
  font-weight: 800;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-invoice-order:hover {
  background: #e2e8f0;
  border-color: #94a3b8;
}

/* Modal */
.modal-backdrop {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1100;
  padding: 1rem;
}

.modal-card {
  background: white;
  border-radius: 20px;
  width: 100%;
  max-width: 500px;
  padding: 1.75rem;
}

.modal-lg {
  max-width: 680px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.25rem;
}

.modal-header h3 {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
}

.btn-close {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 1.1rem;
  cursor: pointer;
}

.modal-body-scroll {
  max-height: 480px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.info-section h4 {
  font-size: 0.95rem;
  font-weight: 800;
  color: #0284c7;
  margin-bottom: 0.75rem;
}

.info-grid {
  background: #f8fafc;
  border-radius: 12px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  font-size: 0.85rem;
}

.table-item-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 0;
  border-bottom: 1px solid #f1f5f9;
}

.table-thumb {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
}

.table-info {
  flex: 1;
}

.table-name {
  font-size: 0.85rem;
  font-weight: 700;
}

.table-spec {
  font-size: 0.75rem;
  color: #64748b;
}

.table-qty {
  font-weight: 700;
}

.table-price {
  font-weight: 800;
  color: #f97316;
}

/* Timeline History */
.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding-left: 0.5rem;
}

.timeline-item {
  display: flex;
  gap: 1rem;
  position: relative;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #0284c7;
  margin-top: 0.25rem;
}

.timeline-content {
  flex: 1;
  background: #f8fafc;
  border-radius: 10px;
  padding: 0.75rem 1rem;
}

.timeline-title {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
  margin-bottom: 0.2rem;
}

.timeline-time {
  font-size: 0.75rem;
  color: #94a3b8;
}

.timeline-desc {
  font-size: 0.8rem;
  color: #475569;
}

.select-reason {
  width: 100%;
  padding: 0.75rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  font-size: 0.9rem;
  outline: none;
}

.margin-top {
  margin-top: 1rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.25rem;
}

.btn-close-modal, .btn-cancel-modal {
  background: #f1f5f9;
  border: none;
  color: #475569;
  padding: 0.75rem 1.25rem;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
}

.btn-confirm-cancel {
  background: #ef4444;
  border: none;
  color: white;
  padding: 0.75rem 1.25rem;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
}

/* Review Modal & Buttons */
.item-actions-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.4rem;
}

.btn-review-item {
  background: #fef3c7;
  border: 1px solid #f59e0b;
  color: #d97706;
  padding: 0.35rem 0.75rem;
  border-radius: 8px;
  font-weight: 800;
  font-size: 0.75rem;
  cursor: pointer;
}

.badge-reviewed-item {
  background: #dcfce7;
  border: 1px solid #86efac;
  color: #16a34a;
  padding: 0.25rem 0.6rem;
  border-radius: 8px;
  font-weight: 800;
  font-size: 0.72rem;
  display: inline-block;
}

.table-price-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.btn-review-item-sm {
  background: #fef3c7;
  border: 1px solid #f59e0b;
  color: #d97706;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  font-weight: 800;
  font-size: 0.7rem;
  cursor: pointer;
}

.badge-reviewed-item-sm {
  background: #dcfce7;
  color: #16a34a;
  padding: 0.15rem 0.45rem;
  border-radius: 6px;
  font-weight: 800;
  font-size: 0.68rem;
}

.review-product-info {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  background: #f8fafc;
  padding: 0.85rem;
  border-radius: 12px;
}

.review-thumb {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  object-fit: cover;
}

.review-prod-title {
  font-size: 0.95rem;
  font-weight: 800;
  color: #0f172a;
}

.star-rating-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #334155;
  margin-bottom: 0.3rem;
}

.star-picker {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.star-icon {
  font-size: 1.8rem;
  color: #cbd5e1;
  cursor: pointer;
  transition: color 0.15s ease;
}

.star-icon.active {
  color: #f59e0b;
}

.star-text-hint {
  font-size: 0.85rem;
  font-weight: 800;
  color: #d97706;
  margin-left: 0.5rem;
}

.review-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 0.75rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  font-size: 0.88rem;
  outline: none;
  font-family: inherit;
}

.btn-confirm-review {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border: none;
  color: white;
  padding: 0.75rem 1.35rem;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
}
</style>

<!-- Feature Implementation: ui danh sách đơn của khách -->

<!-- Feature Implementation: ui chi tiết (sp, ship, giá) -->

<!-- Feature Implementation: ui timeline trạng thái giao hàng -->

<!-- Stage 3: Delivery Timeline Stepper -->