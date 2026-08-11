<script setup>
import { ref } from 'vue';

const props = defineProps({
  order: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['close']);

function formatPrice(val) {
  if (!val && val !== 0) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' });
}

function handlePrint() {
  window.print();
}
</script>

<template>
  <Teleport to="body">
    <div class="invoice-modal-backdrop" @click.self="emit('close')">
    <div class="invoice-modal-card">
      <!-- Toolbar for Actions (Hidden when printing) -->
      <div class="invoice-actions-bar no-print">
        <div class="actions-left">
          <h3>🧾 Xem &amp; In Hóa Đơn Bán Hàng</h3>
        </div>
        <div class="actions-right">
          <button class="btn-print-action" @click="handlePrint">
            🖨️ In Hóa Đơn / Tải PDF
          </button>
          <button class="btn-close-action" @click="emit('close')">
            ✖ Đóng
          </button>
        </div>
      </div>

      <!-- Printable Invoice Sheet -->
      <div class="printable-invoice" id="printable-invoice">
        <!-- Invoice Header -->
        <div class="invoice-header">
          <div class="brand-block">
            <div class="brand-logo">FS</div>
            <div class="brand-text">
              <h1 class="brand-name">FS SHOP</h1>
              <p class="brand-sub">Thời Trang Nam Nữ Cao Cấp - FS Collection</p>
              <p class="store-detail">📍 123 Đường Cầu Giấy, Q. Cầu Giấy, Hà Nội</p>
              <p class="store-detail">📞 Hotline: 0988 777 999 | 🌐 Website: fsshop.com</p>
            </div>
          </div>

          <div class="invoice-meta-block">
            <h2 class="invoice-title">HÓA ĐƠN BÁN HÀNG</h2>
            <div class="meta-row"><span>Mã hóa đơn:</span> <strong>#{{ order.orderCode }}</strong></div>
            <div class="meta-row"><span>Ngày lập:</span> <span>{{ formatDate(order.createdAt) }}</span></div>
            <div class="meta-row"><span>Trạng thái:</span> <span class="status-text">{{ order.orderStatus }}</span></div>
          </div>
        </div>

        <div class="invoice-divider"></div>

        <!-- Customer & Delivery Info Section -->
        <div class="customer-section">
          <div class="info-box">
            <h4>👤 THÔNG TIN KHÁCH HÀNG / THỤ HƯỞNG</h4>
            <div class="info-line"><span>Họ và tên:</span> <strong>{{ order.receiverName }}</strong></div>
            <div class="info-line"><span>Số điện thoại:</span> <strong>{{ order.receiverPhone }}</strong></div>
            <div class="info-line"><span>Địa chỉ giao hàng:</span> <span>{{ order.shippingAddress }}</span></div>
          </div>

          <div class="info-box">
            <h4>💳 THÔNG TIN THANH TOÁN &amp; ĐƠN HÀNG</h4>
            <div class="info-line"><span>Hình thức thanh toán:</span> <strong>{{ order.paymentMethod === 'COD' ? 'Thanh toán khi nhận hàng (COD)' : 'Chuyển khoản Ngân hàng / QR' }}</strong></div>
            <div v-if="order.voucherCode" class="info-line"><span>Mã giảm giá áp dụng:</span> <code class="voucher-code">{{ order.voucherCode }}</code></div>
            <div v-if="order.note" class="info-line"><span>Ghi chú đơn hàng:</span> <em>{{ order.note }}</em></div>
          </div>
        </div>

        <!-- Order Items Table -->
        <table class="invoice-table">
          <thead>
            <tr>
              <th style="width: 40px;" class="text-center">STT</th>
              <th>Sản Phẩm &amp; Phân Loại</th>
              <th style="width: 100px;" class="text-center">Quy Cách</th>
              <th style="width: 110px;" class="text-right">Đơn Giá</th>
              <th style="width: 60px;" class="text-center">SL</th>
              <th style="width: 120px;" class="text-right">Thành Tiền</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in order.items" :key="item.id">
              <td class="text-center">{{ idx + 1 }}</td>
              <td>
                <div class="prod-title">{{ item.productName }}</div>
              </td>
              <td class="text-center">Size: {{ item.size || 'F' }} / {{ item.color || 'Mặc định' }}</td>
              <td class="text-right">{{ formatPrice(item.price) }}</td>
              <td class="text-center font-bold">{{ item.quantity }}</td>
              <td class="text-right font-bold">{{ formatPrice(item.subTotal) }}</td>
            </tr>
          </tbody>
        </table>

        <!-- Calculations & Summary -->
        <div class="summary-section">
          <div class="summary-box">
            <div class="sum-row">
              <span>Tổng cộng giá gốc:</span>
              <strong>{{ formatPrice(order.subTotalAmount || (order.totalAmount + (order.discountAmount || 0))) }}</strong>
            </div>
            <div v-if="order.discountAmount > 0" class="sum-row discount-row">
              <span>Chiết khấu Voucher ({{ order.voucherCode }}):</span>
              <strong class="discount-text">-{{ formatPrice(order.discountAmount) }}</strong>
            </div>
            <div class="sum-row">
              <span>Phí vận chuyển giao hàng:</span>
              <strong>0 ₫ (Miễn phí toàn quốc)</strong>
            </div>
            <div class="sum-row total-row">
              <span>TỔNG THỰC THU (THANH TOÁN):</span>
              <strong class="final-price">{{ formatPrice(order.totalAmount) }}</strong>
            </div>
          </div>
        </div>

        <!-- Signatures & Stamp -->
        <div class="signature-section">
          <div class="sig-box">
            <p class="sig-role">NGƯỜI MUA HÀNG</p>
            <p class="sig-hint">(Ký &amp; ghi rõ họ tên)</p>
            <div class="sig-space"></div>
            <p class="sig-name">{{ order.receiverName }}</p>
          </div>

          <div class="sig-box">
            <p class="sig-role">ĐƠN VỊ BÁN HÀNG - FS SHOP</p>
            <p class="sig-hint">(Ký &amp; đóng dấu xác nhận)</p>
            <div class="sig-space">
              <div class="stamp-badge">FS SHOP VERIFIED</div>
            </div>
            <p class="sig-name">Bộ Phận Quản Lý Đơn Hàng</p>
          </div>
        </div>

        <div class="invoice-footer">
          <p>🌟 <strong>Cảm ơn Quý Khách Đã Lựa Chọn FS SHOP!</strong> 🌟</p>
          <p>Sản phẩm được đổi trả trong vòng 7 ngày nếu có lỗi sản xuất. Hotline hỗ trợ: 0988 777 999</p>
        </div>
      </div>
    </div>
  </div>
  </Teleport>
</template>

<style scoped>
.invoice-modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(5px);
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1.5rem;
  overflow-y: auto;
}

.invoice-modal-card {
  background: white;
  border-radius: 16px;
  max-width: 850px;
  width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.invoice-actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #0f172a;
  color: white;
  padding: 1rem 1.5rem;
}

.invoice-actions-bar h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 800;
}

.actions-right {
  display: flex;
  gap: 0.75rem;
}

.btn-print-action {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
  border: none;
  padding: 0.5rem 1.25rem;
  border-radius: 8px;
  font-weight: 800;
  font-size: 0.85rem;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.btn-print-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.btn-close-action {
  background: #334155;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}

.btn-close-action:hover {
  background: #475569;
}

.printable-invoice {
  padding: 2.5rem;
  overflow-y: auto;
  background: #ffffff;
  color: #1e293b;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* Header */
.invoice-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.brand-block {
  display: flex;
  gap: 1rem;
}

.brand-logo {
  background: linear-gradient(135deg, #0284c7, #2563eb);
  color: white;
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 1.4rem;
}

.brand-name {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 900;
  color: #0f172a;
  letter-spacing: -0.5px;
}

.brand-sub {
  margin: 0.2rem 0 0.5rem 0;
  font-weight: 700;
  color: #0284c7;
  font-size: 0.85rem;
}

.store-detail {
  margin: 0.1rem 0;
  font-size: 0.78rem;
  color: #64748b;
}

.invoice-meta-block {
  text-align: right;
}

.invoice-title {
  margin: 0 0 0.5rem 0;
  font-size: 1.4rem;
  font-weight: 900;
  color: #0f172a;
  letter-spacing: 0.5px;
}

.meta-row {
  font-size: 0.82rem;
  margin-bottom: 0.2rem;
  color: #475569;
}

.status-text {
  font-weight: 800;
  color: #16a34a;
}

.invoice-divider {
  height: 2px;
  background: #e2e8f0;
  margin: 1.5rem 0;
}

/* Customer Section */
.customer-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.info-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 1rem;
}

.info-box h4 {
  margin: 0 0 0.75rem 0;
  font-size: 0.8rem;
  font-weight: 800;
  color: #0f172a;
  border-bottom: 1px solid #cbd5e1;
  padding-bottom: 0.4rem;
}

.info-line {
  font-size: 0.82rem;
  margin-bottom: 0.4rem;
  color: #334155;
}

.voucher-code {
  background: #eff6ff;
  color: #2563eb;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  font-weight: 800;
  border: 1px solid #bfdbfe;
}

/* Table */
.invoice-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1.5rem;
}

.invoice-table th {
  background: #0f172a;
  color: white;
  padding: 0.65rem 0.75rem;
  font-size: 0.8rem;
  font-weight: 800;
  text-align: left;
}

.invoice-table td {
  padding: 0.75rem;
  border-bottom: 1px solid #e2e8f0;
  font-size: 0.83rem;
  color: #334155;
}

.prod-title {
  font-weight: 700;
  color: #0f172a;
}

.text-center { text-align: center; }
.text-right { text-align: right; }
.font-bold { font-weight: 800; }

/* Summary */
.summary-section {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 2rem;
}

.summary-box {
  width: 320px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 1rem;
}

.sum-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
  margin-bottom: 0.5rem;
  color: #475569;
}

.discount-row {
  color: #dc2626;
}

.discount-text {
  color: #dc2626;
}

.total-row {
  border-top: 2px dashed #cbd5e1;
  padding-top: 0.75rem;
  margin-top: 0.5rem;
  font-size: 0.95rem;
  font-weight: 900;
  color: #0f172a;
}

.final-price {
  color: #0284c7;
  font-size: 1.1rem;
}

/* Signature */
.signature-section {
  display: flex;
  justify-content: space-between;
  margin-top: 2rem;
  margin-bottom: 2rem;
}

.sig-box {
  text-align: center;
  width: 220px;
}

.sig-role {
  margin: 0;
  font-weight: 800;
  font-size: 0.85rem;
  color: #0f172a;
}

.sig-hint {
  margin: 0.2rem 0;
  font-size: 0.75rem;
  color: #64748b;
  font-style: italic;
}

.sig-space {
  height: 65px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stamp-badge {
  border: 2px dashed #dc2626;
  color: #dc2626;
  font-weight: 900;
  font-size: 0.75rem;
  padding: 0.3rem 0.8rem;
  border-radius: 6px;
  transform: rotate(-5deg);
  opacity: 0.8;
}

.sig-name {
  margin: 0;
  font-weight: 700;
  font-size: 0.85rem;
  color: #334155;
}

/* Footer Note */
.invoice-footer {
  text-align: center;
  border-top: 1px solid #e2e8f0;
  padding-top: 1rem;
  font-size: 0.78rem;
  color: #64748b;
}

.invoice-footer p {
  margin: 0.2rem 0;
}

</style>

<!-- UNSCOPED GLOBAL MEDIA PRINT RULES FOR CLEAN PDF & PRINT ISOLATION -->
<style>
@media print {
  /* Hide the main application container and any non-modal content */
  #app, body > *:not(.invoice-modal-backdrop) {
    display: none !important;
  }

  body {
    background: #ffffff !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  .invoice-modal-backdrop {
    position: absolute !important;
    top: 0 !important;
    left: 0 !important;
    width: 100% !important;
    height: auto !important;
    background: #ffffff !important;
    backdrop-filter: none !important;
    padding: 0 !important;
    margin: 0 !important;
    display: block !important;
    z-index: 999999 !important;
  }

  .invoice-modal-card {
    max-width: 100% !important;
    max-height: none !important;
    box-shadow: none !important;
    border: none !important;
    border-radius: 0 !important;
    padding: 0 !important;
    margin: 0 !important;
    overflow: visible !important;
  }

  .no-print {
    display: none !important;
  }

  .printable-invoice {
    padding: 15mm !important;
    margin: 0 !important;
    background: white !important;
    color: black !important;
    overflow: visible !important;
  }
}
</style>

<!-- Feature Implementation: thêm modal in hóa đơn pdf cho đơn hàng completed -->
