<script setup>
import { authState } from '../state/authStore';

const emit = defineEmits(['navigate']);
</script>

<template>
  <div class="home-page">
    <!-- Hero Banner Section -->
    <section class="hero-section">
      <div class="hero-content">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          BỘ SƯU TẬP THỜI TRANG MOOD 2026
        </div>
        <h1 class="hero-title">
          Định Hình Phong Cách Với <span class="highlight-orange">FS SHOP</span>
        </h1>
        <p class="hero-subtitle">
          Khám phá những mẫu áo thun, sơ mi, quần jean cao cấp với chất liệu Cotton Premium thoáng mát. 
          Trải nghiệm mua sắm sành điệu với ưu đãi hấp dẫn và dịch vụ giao hàng tận nơi mỗi ngày.
        </p>

        <div class="hero-actions">
          <button class="btn-shop-now" @click="emit('navigate', 'products')">
            KHÁM PHÁ NGAY ➔
          </button>
          <template v-if="!authState.user">
            <button class="btn-secondary" @click="emit('navigate', 'register')">Đăng Ký Tài Khoản</button>
          </template>
        </div>
      </div>

      <div class="hero-image-card">
        <img
          src="https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=800"
          alt="Fashion Clothing Collection"
          class="fashion-hero-img"
        />
        <div class="glass-tag">
          <div class="tag-icon">🔥</div>
          <div>
            <div class="tag-title">Ưu đãi giảm 30%</div>
            <div class="tag-desc">Cho đơn hàng đầu tiên</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Why Choose FS Shop Commercial Features Section -->
    <section class="features-section">
      <div class="section-header">
        <span class="section-tag">DỊCH VỤ HÀNG ĐẦU</span>
        <h2>Tại Sao Bạn Nên Chọn FS SHOP?</h2>
        <p>Thương hiệu thời trang uy tín mang đến trải nghiệm mua sắm vượt trội cho bạn</p>
      </div>

      <div class="features-grid">
        <div class="feature-card">
          <div class="feature-icon-wrapper icon-purple">
            ✨
          </div>
          <h3>Chất Liệu Cao Cấp</h3>
          <p>Chất liệu 100% Cotton Premium thoáng mát, mềm mại và co giãn 4 chiều tạo sự thoải mái cả ngày.</p>
          <div class="feature-tags">
            <span>Cotton Premium</span>
            <span>Thoáng Mát</span>
            <span>Bền Màu</span>
          </div>
        </div>

        <div class="feature-card">
          <div class="feature-icon-wrapper icon-blue">
            ⚡
          </div>
          <h3>Giao Hàng Nhanh Chóng</h3>
          <p>Đơn hàng được xử lý và đóng gói chuyên nghiệp, vận chuyển siêu tốc đến tận tay bạn trên toàn quốc.</p>
          <div class="feature-tags">
            <span>Giao Tận Nơi</span>
            <span>Đóng Gói Đẹp</span>
            <span>Theo Dõi Đơn</span>
          </div>
        </div>

        <div class="feature-card">
          <div class="feature-icon-wrapper icon-orange">
            🎟️
          </div>
          <h3>Voucher &amp; Khuyến Mãi</h3>
          <p>Mã giảm giá đa dạng được cập nhật liên tục, áp dụng trực tiếp vào đơn hàng khi checkout.</p>
          <div class="feature-tags">
            <span>Mã Giảm Giá</span>
            <span>Ưu Đãi VIP</span>
            <span>Tiết Kiệm</span>
          </div>
        </div>

        <div class="feature-card">
          <div class="feature-icon-wrapper icon-green">
            🔄
          </div>
          <h3>Đổi Trả &amp; Hỗ Trợ 24/7</h3>
          <p>Cam kết sản phẩm chuẩn hình 100%, chính sách hỗ trợ đổi trả minh bạch cùng đội ngũ chăm sóc nhiệt tình.</p>
          <div class="feature-tags">
            <span>Đổi Trả 7 Ngày</span>
            <span>Chính Hãng</span>
            <span>Hỗ Trợ 24/7</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem 4rem 1.5rem;
}

/* Hero Section */
.hero-section {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 3rem;
  align-items: center;
  margin-bottom: 5rem;
  background: white;
  padding: 3rem;
  border-radius: 24px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
}

@media (max-width: 900px) {
  .hero-section {
    grid-template-columns: 1fr;
    padding: 2rem;
  }
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: rgba(2, 132, 199, 0.08);
  color: #0284c7;
  padding: 0.4rem 1rem;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin-bottom: 1.25rem;
}

.badge-dot {
  width: 8px;
  height: 8px;
  background: #f97316;
  border-radius: 50%;
}

.hero-title {
  font-size: 2.75rem;
  font-weight: 800;
  line-height: 1.25;
  color: #0f172a;
  margin-bottom: 1.25rem;
}

.highlight-orange {
  color: #f97316;
  background: linear-gradient(120deg, rgba(249, 115, 22, 0.15) 0%, rgba(249, 115, 22, 0) 100%);
  padding: 0 0.4rem;
  border-radius: 6px;
}

.hero-subtitle {
  font-size: 1.05rem;
  color: #475569;
  line-height: 1.6;
  margin-bottom: 2rem;
}

.hero-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.btn-shop-now {
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: white;
  border: none;
  padding: 0.9rem 2rem;
  border-radius: 12px;
  font-weight: 800;
  font-size: 0.95rem;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(249, 115, 22, 0.3);
  transition: transform 0.2s ease;
}

.btn-shop-now:hover {
  transform: translateY(-2px);
}

.btn-secondary {
  background: #f1f5f9;
  color: #0f172a;
  border: 1px solid #cbd5e1;
  padding: 0.9rem 1.8rem;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.95rem;
  cursor: pointer;
  transition: background 0.2s ease;
}

.btn-secondary:hover {
  background: #e2e8f0;
}

.hero-image-card {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}

.fashion-hero-img {
  width: 100%;
  height: 380px;
  object-fit: cover;
  display: block;
}

.glass-tag {
  position: absolute;
  bottom: 1.25rem;
  left: 1.25rem;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  padding: 0.75rem 1.25rem;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.tag-icon {
  font-size: 1.5rem;
}

.tag-title {
  font-weight: 800;
  color: #0f172a;
  font-size: 0.9rem;
}

.tag-desc {
  font-size: 0.75rem;
  color: #64748b;
}

/* Features Section */
.features-section {
  text-align: center;
}

.section-header {
  margin-bottom: 3rem;
}

.section-tag {
  color: #6366f1;
  font-weight: 800;
  font-size: 0.8rem;
  letter-spacing: 1.5px;
}

.section-header h2 {
  font-size: 2.2rem;
  font-weight: 900;
  color: #0f172a;
  margin: 0.5rem 0;
}

.section-header p {
  color: #64748b;
  font-size: 0.95rem;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1.5rem;
}

.feature-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 2rem 1.75rem;
  text-align: left;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  transition: all 0.25s ease;
}

.feature-card:hover {
  transform: translateY(-5px);
  border-color: #6366f1;
  box-shadow: 0 12px 30px rgba(99, 102, 241, 0.12);
}

.feature-icon-wrapper {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  margin-bottom: 1.25rem;
}

.icon-purple { background: #eef2ff; }
.icon-blue { background: #e0f2fe; }
.icon-orange { background: #ffedd5; }
.icon-green { background: #dcfce7; }

.feature-card h3 {
  font-size: 1.15rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 0.5rem;
}

.feature-card p {
  font-size: 0.85rem;
  color: #64748b;
  line-height: 1.5;
  margin-bottom: 1.25rem;
  flex: 1;
}

.feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.feature-tags span {
  background: #f1f5f9;
  color: #475569;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.25rem 0.55rem;
  border-radius: 6px;
}
</style>
