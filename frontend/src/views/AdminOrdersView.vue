<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { adminOrderApi, publicProductApi } from '../services/api';
import { authState } from '../state/authStore';
import InvoiceModal from '../components/InvoiceModal.vue';

const emit = defineEmits(['navigate']);

const orders = ref([]);
const loading = ref(false);
const activeTab = ref('ALL'); // 'ALL', 'PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED'

const selectedOrder = ref(null);
const showDetailModal = ref(false);

// Invoice Modal state
const showInvoiceModal = ref(false);
const invoiceTargetOrder = ref(null);

function openInvoiceModal(order) {
  invoiceTargetOrder.value = order;
  showInvoiceModal.value = true;
}

const adminNote = ref('');
const statusLoading = ref(false);

// Pagination (10 items per page)
const currentPage = ref(1);
const pageSize = 10;

const totalPages = computed(() => {
  return Math.ceil(orders.value.length / pageSize) || 1;
});

const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return orders.value.slice(start, start + pageSize);
});

watch(activeTab, () => {
  currentPage.value = 1;
});

onMounted(async () => {
  if (!authState.user || (authState.user.role !== 'ROLE_ADMIN' && authState.user.role !== 'ROLE_OWNER')) {
    emit('navigate', 'home');
    return;
  }
  await fetchOrders();
});

async function fetchOrders() {
  loading.value = true;
  try {
    const res = await adminOrderApi.getAllOrders(activeTab.value === 'ALL' ? '' : activeTab.value);
    orders.value = res.data || [];
  } catch (err) {
    console.error('Lỗi tải danh sách đơn hàng Admin:', err);
  } finally {
    loading.value = false;
  }
}

async function handleUpdateStatus(orderId, newStatus, noteText = '') {
  statusLoading.value = true;
  try {
    await adminOrderApi.updateOrderStatus(orderId, newStatus, noteText || adminNote.value);
    showDetailModal.value = false;
    adminNote.value = '';
    await fetchOrders();
    alert(`Đã cập nhật tiến trình đơn hàng sang trạng thái "${getStatusText(newStatus)}"!`);
  } catch (err) {
    alert(err.message || 'Cập nhật trạng thái thất bại!');
  } finally {
    statusLoading.value = false;
  }
}

async function handleReviewCancel(orderId, approve) {
  const actionText = approve ? 'ĐỒNG Ý HỦY ĐƠN HÀNG' : 'TỪ CHỐI YÊU CẦU HỦY';
  if (confirm(`Bạn xác nhận ${actionText}?`)) {
    statusLoading.value = true;
    try {
      await adminOrderApi.reviewCancelOrder(orderId, approve, adminNote.value);
      showDetailModal.value = false;
      adminNote.value = '';
      await fetchOrders();
      alert(`Đã xử lý: ${actionText}`);
    } catch (err) {
      alert(err.message || 'Xử lý thất bại!');
    } finally {
      statusLoading.value = false;
    }
  }
}

function openDetailModal(order) {
  selectedOrder.value = order;
  adminNote.value = '';
  showDetailModal.value = true;
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
    case 'PENDING': return '⏳ Chờ Duyệt';
    case 'CONFIRMED': return '✅ Đã Duyệt';
    case 'SHIPPING': return '🚚 Đang Giao';
    case 'DELIVERED': return '📦 Đã Giao (Chờ Khách)';
    case 'COMPLETED': return '🎉 Hoàn Thành';
    case 'CANCELLED': return '❌ Đã Hủy';
    case 'CANCEL_REQUESTED': return '⚠️ Yêu Cầu Hủy';
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

async function goToProductDetail(itemOrId) {
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
</script>

<template>
  <div class="admin-orders-page">
    <div class="page-header">
      <div>
        <span class="badge-admin">QUẢN TRỊ ĐƠN HÀNG (ADMIN)</span>
        <h1>Quản Lý Tiến Trình Đơn Hàng</h1>
        <p>Xác nhận đơn hàng, duyệt giao hàng và quản lý tiến trình giao hàng toàn hệ thống</p>
      </div>
    </div>

    <!-- Status Filter Tabs -->
    <div class="status-tabs">
      <button :class="['tab-btn', activeTab === 'ALL' ? 'active' : '']" @click="activeTab = 'ALL'; fetchOrders()">
        Tất Cả Đơn
      </button>
      <button :class="['tab-btn', activeTab === 'PENDING' ? 'active' : '']" @click="activeTab = 'PENDING'; fetchOrders()">
        ⏳ Chờ Duyệt
      </button>
      <button :class="['tab-btn', activeTab === 'CONFIRMED' ? 'active' : '']" @click="activeTab = 'CONFIRMED'; fetchOrders()">
        ✅ Đã Duyệt
      </button>
      <button :class="['tab-btn', activeTab === 'SHIPPING' ? 'active' : '']" @click="activeTab = 'SHIPPING'; fetchOrders()">
        🚚 Đang Giao
      </button>
      <button :class="['tab-btn', activeTab === 'COMPLETED' ? 'active' : '']" @click="activeTab = 'COMPLETED'; fetchOrders()">
        🎉 Hoàn Thành
      </button>
      <button :class="['tab-btn', activeTab === 'CANCELLED' ? 'active' : '']" @click="activeTab = 'CANCELLED'; fetchOrders()">
        ❌ Đã Hủy
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      ⏳ Đang tải danh sách đơn hàng...
    </div>

    <!-- Empty -->
    <div v-else-if="!orders.length" class="empty-orders">
      <div class="empty-icon">📦</div>
      <h3>Không có đơn hàng nào ở trạng thái này</h3>
    </div>

    <!-- Admin Orders Table -->
    <div v-else class="orders-table-card">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Mã Đơn</th>
            <th>Người Nhận &amp; SĐT</th>
            <th>Địa Chỉ Giao Hàng</th>
            <th>Tổng Tiền</th>
            <th>Thanh Toán</th>
            <th>Trạng Thái</th>
            <th>Hành Động Duyệt</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in paginatedOrders" :key="order.id">
            <td>
              <strong class="order-code-text" @click="openDetailModal(order)">{{ order.orderCode }}</strong>
              <div class="order-time">{{ formatDate(order.createdAt) }}</div>
            </td>
            <td>
              <strong>{{ order.receiverName }}</strong>
              <div class="text-sm text-gray">{{ order.receiverPhone }}</div>
            </td>
            <td class="address-cell">
              {{ order.shippingAddress }}
            </td>
            <td>
              <strong class="text-orange">{{ formatPrice(order.totalAmount) }}</strong>
            </td>
            <td>
              <span :class="['payment-tag', order.paymentStatus === 'PAID' ? 'paid' : 'unpaid']">
                {{ order.paymentStatus === 'PAID' ? '✅ ĐÃ THANH TOÁN' : '⏳ CHƯA THANH TOÁN' }}
              </span>
              <div class="text-xs text-gray">{{ order.paymentMethod }}</div>
            </td>
            <td>
              <span :class="['status-badge', getStatusBadgeClass(order.orderStatus)]">
                {{ getStatusText(order.orderStatus) }}
              </span>
            </td>
            <td>
              <div class="admin-action-btns">
                <!-- Step 1: Confirm Order -->
                <button
                  v-if="order.orderStatus === 'PENDING'"
                  class="btn-action-confirm"
                  @click="handleUpdateStatus(order.id, 'CONFIRMED', 'Admin đã duyệt đơn hàng.')"
                >
                  ✅ DUYỆT ĐƠN
                </button>

                <!-- Step 2: Start Shipping -->
                <button
                  v-if="order.orderStatus === 'CONFIRMED'"
                  class="btn-action-ship"
                  @click="handleUpdateStatus(order.id, 'SHIPPING', 'Đơn hàng đã bàn giao cho shippper giao đi.')"
                >
                  🚚 GIAO HÀNG
                </button>

                <!-- Step 3: Admin Confirms Delivery (1-Side Confirmation) -->
                <button
                  v-if="order.orderStatus === 'SHIPPING'"
                  class="btn-action-complete"
                  @click="handleUpdateStatus(order.id, 'DELIVERED', 'Admin báo đã giao hàng thành công (Xác nhận 1 phía). Chờ khách hàng xác nhận nhận hàng.')"
                >
                  📦 BÁO ĐÃ GIAO HÀNG
                </button>

                <!-- Review Cancellation Request from Customer -->
                <template v-if="order.orderStatus === 'CANCEL_REQUESTED'">
                  <button
                    class="btn-action-cancel"
                    @click="handleReviewCancel(order.id, true)"
                  >
                    ✅ DUYỆT HỦY ĐƠN
                  </button>
                  <button
                    class="btn-action-ship"
                    @click="handleReviewCancel(order.id, false)"
                  >
                    ❌ TỪ CHỐI HỦY
                  </button>
                </template>

                <button 
                  v-if="['CONFIRMED', 'DELIVERED', 'COMPLETED'].includes(order.orderStatus)" 
                  class="btn-action-invoice" 
                  @click="openInvoiceModal(order)"
                >
                  🧾 In Hóa Đơn
                </button>

                <button class="btn-action-detail" @click="openDetailModal(order)">
                  📋 Chi Tiết
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination Controls -->
    <div class="pagination-wrapper" v-if="totalPages > 1">
      <button 
        class="page-btn" 
        :disabled="currentPage === 1" 
        @click="currentPage--"
      >
        &laquo; Trước
      </button>
      <button 
        v-for="page in totalPages" 
        :key="page" 
        :class="['page-btn', currentPage === page ? 'active' : '']"
        @click="currentPage = page"
      >
        {{ page }}
      </button>
      <button 
        class="page-btn" 
        :disabled="currentPage === totalPages" 
        @click="currentPage++"
      >
        Sau &raquo;
      </button>
    </div>

    <!-- Order Detail & Status Update Modal -->
    <div v-if="showDetailModal && selectedOrder" class="modal-backdrop" @click.self="showDetailModal = false">
      <div class="modal-card modal-admin-xl">
        <div class="modal-header">
          <div class="modal-header-title-box">
            <h3>📋 Quản Lý Đơn Hàng: <span class="highlight-code">#{{ selectedOrder.orderCode }}</span></h3>
            <span :class="['status-badge', getStatusBadgeClass(selectedOrder.orderStatus)]">
              {{ getStatusText(selectedOrder.orderStatus) }}
            </span>
          </div>
          <button class="btn-close" @click="showDetailModal = false">✖</button>
        </div>

        <div class="modal-body-scroll">
          <!-- 2-Column Top Layout: Customer Info & Order Financial Summary -->
          <div class="modal-top-grid">
            <!-- Left Box: Receiver & Shipping Details -->
            <div class="card-info-box">
              <h4 class="box-title">👤 Thông Tin Nhận Hàng</h4>
              <div class="info-details-list">
                <div class="detail-row">
                  <span class="detail-label">Người nhận:</span>
                  <strong class="detail-val">{{ selectedOrder.receiverName }}</strong>
                </div>
                <div class="detail-row">
                  <span class="detail-label">Số điện thoại:</span>
                  <strong class="detail-val text-phone">📞 {{ selectedOrder.receiverPhone }}</strong>
                </div>
                <div class="detail-row">
                  <span class="detail-label">Địa chỉ giao:</span>
                  <span class="detail-val">📍 {{ selectedOrder.shippingAddress }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">Thanh toán:</span>
                  <span class="detail-val">
                    <strong class="payment-tag">{{ selectedOrder.paymentMethod === 'COD' ? '💵 COD (Thanh toán khi nhận hàng)' : '📲 Chuyển khoản QR' }}</strong>
                  </span>
                </div>
                <div v-if="selectedOrder.note" class="detail-row">
                  <span class="detail-label">Ghi chú của khách:</span>
                  <em class="detail-val note-text">"{{ selectedOrder.note }}"</em>
                </div>
              </div>
            </div>

            <!-- Right Box: Financial & Order Summary Card -->
            <div class="card-info-box summary-card-box">
              <h4 class="box-title">💰 Tổng Quan Tài Chính Đơn Hàng</h4>
              <div class="summary-details-list">
                <div class="detail-row">
                  <span class="detail-label">Thời gian đặt:</span>
                  <span class="detail-val">{{ formatDate(selectedOrder.createdAt) }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">Số lượng sản phẩm:</span>
                  <strong class="detail-val">{{ selectedOrder.items ? selectedOrder.items.reduce((acc, i) => acc + i.quantity, 0) : 0 }} SP</strong>
                </div>
                <div v-if="selectedOrder.voucherCode" class="detail-row">
                  <span class="detail-label">Mã Voucher áp dụng:</span>
                  <span class="detail-val voucher-badge">🎟️ {{ selectedOrder.voucherCode }} (-{{ formatPrice(selectedOrder.discountAmount) }})</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">Phí vận chuyển:</span>
                  <span class="detail-val text-green">Miễn phí 0 ₫</span>
                </div>
                <div class="total-price-banner">
                  <span class="total-banner-label">TỔNG THỰC THU:</span>
                  <span class="total-banner-val">{{ formatPrice(selectedOrder.totalAmount) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Product Items Grid View (Visually rich Product Cards) -->
          <div class="info-section">
            <h4 class="section-heading">🛒 Danh Sách Sản Phẩm Đặt Mua ({{ selectedOrder.items ? selectedOrder.items.length : 0 }})</h4>
            <div class="products-grid-view">
              <div v-for="item in selectedOrder.items" :key="item.id" class="admin-prod-card">
                <div class="prod-card-thumb-wrapper" @click="goToProductDetail(item)" style="cursor: pointer;" title="Xem chi tiết & mua sản phẩm">
                  <img :src="item.thumbnailUrl || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=300'" class="prod-card-thumb" />
                  <span class="prod-qty-badge">x{{ item.quantity }}</span>
                </div>

                <div class="prod-card-info">
                  <h5 class="prod-card-title clickable-title" @click="goToProductDetail(item)" title="Xem chi tiết & mua sản phẩm">
                    {{ item.productName }}
                  </h5>
                  <div class="prod-specs-pills">
                    <span class="spec-pill">Size: {{ item.size || 'F' }}</span>
                    <span class="spec-pill">Màu: {{ item.color || 'Mặc định' }}</span>
                  </div>
                  <div class="prod-card-price-row">
                    <span class="prod-unit-price">{{ formatPrice(item.price) }}</span>
                    <strong class="prod-subtotal-price">{{ formatPrice(item.subTotal) }}</strong>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Admin Quick Status Update Box -->
          <div class="info-section admin-update-box">
            <h4 class="section-heading">⚡ Cập Nhật Tiến Trình Đơn Hàng (Admin & Owner)</h4>
            <div class="note-form-group">
              <label class="note-label">Ghi chú tiến trình (Tùy chọn):</label>
              <input v-model="adminNote" type="text" class="note-input" placeholder="Nhập ghi chú giao hàng / xác nhận..." />
            </div>

            <div class="update-btns-row">
              <!-- PENDING -> CONFIRMED -->
              <button
                v-if="selectedOrder.orderStatus === 'PENDING'"
                class="btn-action-confirm"
                @click="handleUpdateStatus(selectedOrder.id, 'CONFIRMED', adminNote)"
              >
                ✅ XÁC NHẬN ĐƠN HÀNG (CONFIRMED)
              </button>

              <!-- CONFIRMED -> SHIPPING -->
              <button
                v-if="selectedOrder.orderStatus === 'CONFIRMED'"
                class="btn-action-ship"
                @click="handleUpdateStatus(selectedOrder.id, 'SHIPPING', adminNote)"
              >
                🚚 GIAO HÀNG CHO ĐƠN VỊ VẬN CHUYỂN (SHIPPING)
              </button>

              <!-- SHIPPING -> DELIVERED -->
              <button
                v-if="selectedOrder.orderStatus === 'SHIPPING'"
                class="btn-action-complete"
                @click="handleUpdateStatus(selectedOrder.id, 'DELIVERED', adminNote)"
              >
                📦 ĐÃ GIAO HÀNG THÀNH CÔNG (DELIVERED)
              </button>

              <!-- Review Cancel Requested Order -->
              <template v-if="selectedOrder.orderStatus === 'CANCEL_REQUESTED'">
                <button class="btn-action-confirm" @click="handleReviewCancel(selectedOrder.id, true)">
                  ✔️ Chấp Nhận Hủy Đơn
                </button>
                <button class="btn-action-ship" @click="handleReviewCancel(selectedOrder.id, false)">
                  ❌ Từ Chối Yêu Cầu Hủy
                </button>
              </template>

              <!-- Admin Unilateral Cancel Button -->
              <button
                v-if="selectedOrder.orderStatus !== 'COMPLETED' && selectedOrder.orderStatus !== 'CANCELLED' && selectedOrder.orderStatus !== 'CANCEL_REQUESTED'"
                class="btn-action-cancel"
                @click="handleUpdateStatus(selectedOrder.id, 'CANCELLED', 'Admin đơn phương hủy đơn hàng')"
              >
                🚫 Hủy Đơn Hàng (Đơn Phương)
              </button>
            </div>
          </div>

          <!-- Timeline -->
          <div class="info-section">
            <h4 class="section-heading">⏱️ Lịch Sử Tiến Độ Đơn Hàng (Timeline)</h4>
            <div class="timeline-list">
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
          </div>
        </div>

        <div class="modal-footer">
          <button 
            v-if="['CONFIRMED', 'DELIVERED', 'COMPLETED'].includes(selectedOrder.orderStatus)" 
            class="btn-action-invoice-lg" 
            @click="openInvoiceModal(selectedOrder)"
          >
            🧾 IN HÓA ĐƠN BÁN HÀNG (PDF)
          </button>
          <button class="btn-close-modal" @click="showDetailModal = false">Đóng Cửa Sổ</button>
        </div>
      </div>
    </div>

    <!-- Printable Invoice Modal Overlay -->
    <InvoiceModal
      v-if="showInvoiceModal && invoiceTargetOrder"
      :order="invoiceTargetOrder"
      @close="showInvoiceModal = false"
    />
  </div>
</template>

<style scoped>
.admin-orders-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem;
}

.page-header {
  margin-bottom: 2rem;
}

.badge-admin {
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
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
}

.tab-btn.active {
  background: #f97316;
  color: white;
  border-color: #f97316;
}

.orders-table-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  overflow-x: auto;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
}

.admin-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.9rem;
}

.admin-table th {
  background: #f8fafc;
  color: #475569;
  font-weight: 800;
  padding: 1rem;
  border-bottom: 1px solid #e2e8f0;
}

.admin-table td {
  padding: 1rem;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.order-code-text {
  color: #0284c7;
  cursor: pointer;
}

.order-time {
  font-size: 0.75rem;
  color: #94a3b8;
}

.address-cell {
  max-width: 220px;
  font-size: 0.85rem;
  color: #475569;
}

.payment-tag {
  font-size: 0.7rem;
  font-weight: 800;
  padding: 0.15rem 0.45rem;
  border-radius: 6px;
  white-space: nowrap;
  display: inline-block;
}
.payment-tag.paid { background: #dcfce7; color: #16a34a; }
.payment-tag.unpaid { background: #fef3c7; color: #d97706; }

/* Status Badges */
.status-badge {
  padding: 0.3rem 0.65rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 800;
  white-space: nowrap;
  display: inline-block;
}
.status-pending { background: #fef3c7; color: #d97706; }
.status-confirmed { background: #e0f2fe; color: #0284c7; }
.status-shipping { background: #e0e7ff; color: #4338ca; }
.status-delivered { background: #dbeafe; color: #1d4ed8; }
.status-completed { background: #dcfce7; color: #15803d; }
.status-cancelled { background: #ffe4e6; color: #e11d48; }
.status-cancel-req { background: #fef2f2; color: #dc2626; border: 1px dashed #ef4444; }

.admin-action-btns {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.btn-action-confirm {
  background: #0284c7;
  color: white;
  border: none;
  padding: 0.4rem 0.75rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.75rem;
  cursor: pointer;
}

.btn-action-ship {
  background: #4338ca;
  color: white;
  border: none;
  padding: 0.4rem 0.75rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.75rem;
  cursor: pointer;
}

.btn-action-complete {
  background: #16a34a;
  color: white;
  border: none;
  padding: 0.4rem 0.75rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.75rem;
  cursor: pointer;
}

.btn-action-cancel {
  background: #ef4444;
  color: white;
  border: none;
  padding: 0.4rem 0.75rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.75rem;
  cursor: pointer;
}

.btn-action-detail {
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 0.4rem 0.75rem;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
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
  max-width: 680px;
  padding: 1.75rem;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.25rem;
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

.admin-update-box {
  background: #fff7ed;
  border: 1px solid #ffedd5;
  border-radius: 14px;
  padding: 1.25rem;
}

.note-form-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-top: 0.5rem;
}

.note-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #475569;
}

.note-input {
  width: 100%;
  box-sizing: border-box;
  padding: 0.65rem 0.9rem;
  border: 1.5px solid #fed7aa;
  border-radius: 10px;
  font-size: 0.88rem;
  outline: none;
  background: white;
}

.btn-action-invoice {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
  padding: 0.4rem 0.75rem;
  border-radius: 8px;
  font-size: 0.78rem;
  font-weight: 800;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-action-invoice:hover {
  background: #dbeafe;
}

.text-delivered-notice {
  font-size: 0.85rem;
  font-weight: 800;
  color: #1d4ed8;
  background: #eff6ff;
  padding: 0.5rem 0.8rem;
  border-radius: 8px;
  border: 1px solid #bfdbfe;
}

.update-btns-row {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-top: 0.85rem;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding-left: 0.5rem;
}

.timeline-item {
  display: flex;
  gap: 1rem;
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
}

.timeline-time {
  font-size: 0.75rem;
  color: #94a3b8;
}

.timeline-desc {
  font-size: 0.8rem;
  color: #475569;
}

.modal-admin-xl {
  max-width: 980px !important;
  width: 95% !important;
}

.modal-header-title-box {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.highlight-code {
  color: #0284c7;
  font-family: monospace;
}

.modal-top-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
  margin-bottom: 1.5rem;
}

.card-info-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 1.1rem;
}

.box-title {
  margin: 0 0 0.85rem 0;
  font-size: 0.92rem;
  font-weight: 800;
  color: #0f172a;
  border-bottom: 1px dashed #cbd5e1;
  padding-bottom: 0.4rem;
}

.info-details-list {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.detail-row {
  font-size: 0.85rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-label {
  color: #64748b;
}

.detail-val {
  color: #1e293b;
}

.voucher-badge {
  background: #eff6ff;
  color: #2563eb;
  padding: 0.1rem 0.45rem;
  border-radius: 6px;
  font-weight: 800;
  border: 1px solid #bfdbfe;
}

.text-green {
  color: #16a34a;
  font-weight: 800;
}

.total-price-banner {
  background: linear-gradient(135deg, #0f172a, #1e293b);
  color: white;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.75rem;
}

.total-banner-label {
  font-size: 0.85rem;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.total-banner-val {
  font-size: 1.2rem;
  font-weight: 900;
  color: #38bdf8;
}

/* Products Grid View */
.section-heading {
  margin: 0 0 0.75rem 0;
  font-size: 0.95rem;
  font-weight: 800;
  color: #0f172a;
}

.products-grid-view {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
  margin-top: 0.75rem;
}

.admin-prod-card {
  display: flex;
  gap: 0.85rem;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 0.75rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.03);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.admin-prod-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0,0,0,0.06);
  border-color: #cbd5e1;
}

.prod-card-thumb-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  flex-shrink: 0;
}

.prod-card-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
}

.prod-qty-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #0284c7;
  color: white;
  font-size: 0.72rem;
  font-weight: 900;
  padding: 0.1rem 0.4rem;
  border-radius: 20px;
  border: 2px solid white;
}

.prod-card-info {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  flex: 1;
}

.prod-card-title {
  margin: 0 0 0.35rem 0;
  font-size: 0.85rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.25;
}

.clickable-title {
  cursor: pointer;
  color: #0f172a;
  font-weight: 600;
  transition: all 0.15s ease;
}

.clickable-title:hover {
  color: #0284c7;
  font-weight: 800;
  text-decoration: underline;
}

.prod-specs-pills {
  display: flex;
  gap: 0.35rem;
  margin-bottom: 0.35rem;
}

.spec-pill {
  background: #f1f5f9;
  color: #475569;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 6px;
}

.prod-card-price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.prod-unit-price {
  font-size: 0.75rem;
  color: #94a3b8;
}

.prod-subtotal-price {
  font-size: 0.9rem;
  color: #0f172a;
  font-weight: 900;
}

.btn-action-invoice-lg {
  background: linear-gradient(135deg, #0284c7, #2563eb);
  color: white;
  border: none;
  padding: 0.65rem 1.35rem;
  border-radius: 10px;
  font-weight: 800;
  font-size: 0.88rem;
  cursor: pointer;
  transition: transform 0.15s ease;
  margin-right: auto;
}

.btn-action-invoice-lg:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(2, 132, 199, 0.3);
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.25rem;
}

.btn-close-modal {
  background: #f1f5f9;
  border: none;
  color: #475569;
  padding: 0.75rem 1.25rem;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
}
</style>

<!-- Feature Implementation: ui/api admin duyệt đơn -->
