<script setup>
import { defineProps } from 'vue';

const props = defineProps({
  order: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['navigate']);

function formatPrice(val) {
  if (!val) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}
</script>

<template>
  <div class="success-page">
    <div class="success-card">
      <div class="icon-circle">✅</div>
      <span class="badge-success">ĐẶT HÀNG THÀNH CÔNG</span>
      <h2>Cảm Ơn Bạn Đã Mua Sắm Tại FS SHOP!</h2>
      <p class="subtitle">Đơn hàng của bạn đã được ghi nhận vào hệ thống và đang chờ cửa hàng xác nhận.</p>

      <div v-if="order" class="order-details-box">
        <div class="order-code-badge">
          MÃ ĐƠN HÀNG: <strong>{{ order.orderCode }}</strong>
        </div>

        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">Người nhận:</span>
            <span class="val">{{ order.receiverName }} ({{ order.receiverPhone }})</span>
          </div>

          <div class="detail-item">
            <span class="label">Địa chỉ giao:</span>
            <span class="val">{{ order.shippingAddress }}</span>
          </div>

          <div class="detail-item">
            <span class="label">Phương thức thanh toán:</span>
            <span class="val font-bold text-blue">
              {{ order.paymentMethod === 'COD' ? '💵 Thanh toán khi nhận hàng (COD)' : '📲 Chuyển khoản QR / VNPAY Demo' }}
            </span>
          </div>

          <div class="detail-item">
            <span class="label">Tổng tiền thanh toán:</span>
            <span class="val total-amount">{{ formatPrice(order.totalAmount) }}</span>
          </div>
        </div>

        <!-- Order Status Timeline (US21 Preview) -->
        <div class="timeline-box">
          <div class="timeline-step active">
            <div class="step-dot">1</div>
            <div class="step-text">Đã Đặt Hàng</div>
          </div>
          <div class="timeline-line"></div>
          <div class="timeline-step">
            <div class="step-dot">2</div>
            <div class="step-text">Xác Nhận</div>
          </div>
          <div class="timeline-line"></div>
          <div class="timeline-step">
            <div class="step-dot">3</div>
            <div class="step-text">Đang Giao</div>
          </div>
          <div class="timeline-line"></div>
          <div class="timeline-step">
            <div class="step-dot">4</div>
            <div class="step-text">Hoàn Thành</div>
          </div>
        </div>
      </div>

      <div class="action-buttons">
        <button class="btn-home" @click="emit('navigate', 'home')">
          🛍️ Tiếp Tục Mua Sắm
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.success-page {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 3rem 1.5rem;
}

.success-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 3rem 2rem;
  max-width: 620px;
  width: 100%;
  text-align: center;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.05);
}

.icon-circle {
  font-size: 3.5rem;
  margin-bottom: 0.5rem;
}

.badge-success {
  display: inline-block;
  background: #dcfce7;
  color: #15803d;
  font-weight: 800;
  font-size: 0.75rem;
  padding: 0.3rem 0.8rem;
  border-radius: 20px;
  letter-spacing: 1px;
  margin-bottom: 0.75rem;
}

.success-card h2 {
  font-size: 1.75rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 0.4rem;
}

.subtitle {
  color: #64748b;
  font-size: 0.9rem;
  margin-bottom: 2rem;
}

.order-details-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 1.5rem;
  margin-bottom: 2rem;
  text-align: left;
}

.order-code-badge {
  text-align: center;
  background: white;
  border: 1.5px dashed #0284c7;
  color: #0284c7;
  padding: 0.6rem;
  border-radius: 10px;
  font-weight: 800;
  font-size: 1rem;
  margin-bottom: 1.25rem;
}

.detail-grid {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  font-size: 0.9rem;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 0.5rem;
}

.label {
  color: #64748b;
}

.val {
  color: #0f172a;
  font-weight: 600;
}

.total-amount {
  font-size: 1.2rem;
  font-weight: 800;
  color: #f97316;
}

/* Timeline */
.timeline-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid #e2e8f0;
}

.timeline-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
}

.step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #cbd5e1;
  color: white;
  font-weight: 800;
  font-size: 0.8rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.timeline-step.active .step-dot {
  background: #10b981;
}

.step-text {
  font-size: 0.7rem;
  font-weight: 700;
  color: #94a3b8;
}

.timeline-step.active .step-text {
  color: #10b981;
}

.timeline-line {
  flex: 1;
  height: 2px;
  background: #e2e8f0;
  margin: 0 0.4rem;
  margin-bottom: 1rem;
}

.action-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.btn-home {
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: white;
  border: none;
  padding: 0.85rem 2rem;
  border-radius: 12px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(249, 115, 22, 0.25);
}
</style>
