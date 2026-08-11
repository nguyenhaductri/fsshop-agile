<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { adminDashboardApi } from '../services/api';
import { authState } from '../state/authStore';

const emit = defineEmits(['navigate']);

const summary = ref(null);
const loading = ref(false);
const selectedYear = ref(new Date().getFullYear());

const years = computed(() => {
  const cur = new Date().getFullYear();
  return [cur, cur - 1, cur - 2, cur - 3];
});

onMounted(async () => {
  if (!authState.user || (authState.user.role !== 'ROLE_ADMIN' && authState.user.role !== 'ROLE_OWNER')) {
    emit('navigate', 'home');
    return;
  }
  await fetchSummary();
});

async function changeYear(year) {
  selectedYear.value = year;
  await fetchSummary();
}

const maxMonthlyRevenue = computed(() => {
  if (!summary.value || !summary.value.monthlyRevenues) return 1;
  const max = Math.max(...summary.value.monthlyRevenues.map(m => m.revenue || 0));
  return max || 1;
});

function formatPrice(val) {
  if (!val && val !== 0) return '0 ₫';
  if (val >= 1_000_000_000) {
    return (val / 1_000_000_000).toFixed(1) + ' Tỷ ₫';
  }
  if (val >= 1_000_000) {
    return (val / 1_000_000).toFixed(1) + ' Triệu ₫';
  }
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}

function formatFullPrice(val) {
  if (!val && val !== 0) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}

function barHeightPercent(revenue) {
  return Math.round((revenue / maxMonthlyRevenue.value) * 100);
}

// Full Products List features
const searchKeyword = ref('');
const sortOrder = ref('desc'); // 'desc' or 'asc'
const selectedPeriod = ref('all'); // 'all', 'year', 'month', 'today', 'custom'
const customFrom = ref('2024-06');
const customTo = ref('2026-01');

async function fetchSummary() {
  loading.value = true;
  try {
    const params = { year: selectedYear.value };
    if (selectedPeriod.value === 'custom') {
      params.fromDate = customFrom.value;
      params.toDate = customTo.value;
    }
    const res = await adminDashboardApi.getSummary(params);
    summary.value = res.data;
  } catch (err) {
    console.error('Lỗi tải thống kê:', err);
  } finally {
    loading.value = false;
  }
}

async function handlePeriodChange() {
  await fetchSummary();
}

async function applyCustomDateRange() {
  if (selectedPeriod.value === 'custom') {
    await fetchSummary();
  }
}

function getQtyByPeriod(p, period) {
  if (period === 'today') return p.quantitySoldToday || 0;
  if (period === 'month') return p.quantitySoldMonth || 0;
  if (period === 'year') return p.quantitySoldYear || 0;
  if (period === 'custom') return p.quantitySoldCustom || 0;
  return p.quantitySoldAll || p.quantitySold || 0;
}

function getRevenueByPeriod(p, period) {
  if (period === 'today') return p.revenueToday || 0;
  if (period === 'month') return p.revenueMonth || 0;
  if (period === 'year') return p.revenueYear || 0;
  if (period === 'custom') return p.revenueCustom || 0;
  return p.revenueAll || p.revenue || 0;
}

const allProductsWithRank = computed(() => {
  if (!summary.value || !summary.value.topProducts) return [];
  return summary.value.topProducts.map((p, index) => ({
    ...p,
    originalRank: index + 1
  }));
});

const activeSummary = computed(() => {
  if (!summary.value) return {
    totalRevenue: 0,
    completedOrders: 0,
    productsSold: 0,
    cancelledOrders: 0,
    totalOrders: 0,
    cancellationRate: 0
  };

  if (selectedPeriod.value === 'today') return summary.value.summaryToday || {};
  if (selectedPeriod.value === 'month') return summary.value.summaryMonth || {};
  if (selectedPeriod.value === 'year') return summary.value.summaryYear || {};
  if (selectedPeriod.value === 'custom') return summary.value.summaryCustom || {};
  return summary.value.summaryAll || {
    totalRevenue: summary.value.totalRevenue,
    completedOrders: summary.value.totalCompletedOrders,
    productsSold: summary.value.totalProductsSold,
    cancelledOrders: 0,
    totalOrders: summary.value.totalCompletedOrders,
    cancellationRate: 0
  };
});

const periodLabel = computed(() => {
  if (selectedPeriod.value === 'today') return 'Hôm nay';
  if (selectedPeriod.value === 'month') return 'Tháng này';
  if (selectedPeriod.value === 'year') return `Năm ${selectedYear.value}`;
  if (selectedPeriod.value === 'custom') return `Từ ${customFrom.value || '...'} đến ${customTo.value || '...'}`;
  return 'Tất cả thời gian';
});

const filteredAndSortedProducts = computed(() => {
  let list = allProductsWithRank.value.map(p => ({
    ...p,
    displayQty: getQtyByPeriod(p, selectedPeriod.value),
    displayRevenue: getRevenueByPeriod(p, selectedPeriod.value)
  }));

  // Filter by keyword
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.toLowerCase().trim();
    list = list.filter(p => p.productName.toLowerCase().includes(kw));
  }

  // Sort
  list.sort((a, b) => {
    if (sortOrder.value === 'desc') {
      return b.displayQty - a.displayQty;
    } else {
      return a.displayQty - b.displayQty;
    }
  });

  return list;
});

// Pagination features (10 items per page)
const currentPage = ref(1);
const pageSize = 10;

const totalPages = computed(() => {
  return Math.ceil(filteredAndSortedProducts.value.length / pageSize) || 1;
});

const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredAndSortedProducts.value.slice(start, start + pageSize);
});

watch([searchKeyword, sortOrder, selectedPeriod], () => {
  currentPage.value = 1;
});

function goToProductDetail(productId) {
  if (productId) {
    emit('navigate', 'product-detail', productId);
  }
}
</script>

<template>
  <div class="dashboard-page">
    <!-- Page Header -->
    <div class="page-header">
      <div>
        <div class="page-badge">ADMIN • PHÂN TÍCH</div>
        <h1 class="page-title">📊 Thống Kê Doanh Thu</h1>
        <p class="page-subtitle">Tổng hợp doanh thu từ các đơn hàng đã giao thành công</p>
      </div>

      <!-- Header Controls: Year & Period Picker -->
      <div class="header-controls">
        <div class="period-picker">
          <span class="picker-label">Lọc thời gian:</span>
          <select v-model="selectedPeriod" @change="handlePeriodChange" class="header-select">
            <option value="all">🌐 Tất cả thời gian</option>
            <option value="year">📅 Năm {{ selectedYear }}</option>
            <option value="month">🗓️ Tháng này</option>
            <option value="today">☀️ Hôm nay</option>
            <option value="custom">⚙️ Tùy chỉnh (Từ... Đến...)</option>
          </select>
        </div>

        <div v-if="selectedPeriod === 'custom'" class="custom-range-picker">
          <span class="picker-label">Từ:</span>
          <input type="month" v-model="customFrom" class="date-input" />
          <span class="picker-label">Đến:</span>
          <input type="month" v-model="customTo" class="date-input" />
          <button @click="applyCustomDateRange" class="btn-apply-range">Áp dụng</button>
        </div>

        <div v-if="selectedPeriod !== 'custom'" class="year-picker">
          <span class="year-label">Năm:</span>
          <button
            v-for="y in years"
            :key="y"
            :class="['year-btn', selectedYear === y ? 'active' : '']"
            @click="changeYear(y)"
          >
            {{ y }}
          </button>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="spinner-lg"></div>
      <p>Đang tải dữ liệu thống kê...</p>
    </div>

    <template v-else-if="summary">
      <!-- Summary Cards Row -->
      <div class="summary-cards">
        <div class="summary-card card-revenue">
          <div class="card-icon">💰</div>
          <div class="card-body">
            <div class="card-label">Tổng Doanh Thu</div>
            <div class="card-value">{{ formatPrice(activeSummary.totalRevenue) }}</div>
            <div class="card-hint">Theo: {{ periodLabel }}</div>
          </div>
        </div>

        <div class="summary-card card-orders">
          <div class="card-icon">📦</div>
          <div class="card-body">
            <div class="card-label">Đơn Thành Công</div>
            <div class="card-value">{{ activeSummary.completedOrders?.toLocaleString('vi-VN') || 0 }}</div>
            <div class="card-hint">Đã giao thành công ({{ periodLabel }})</div>
          </div>
        </div>

        <div class="summary-card card-products">
          <div class="card-icon">👕</div>
          <div class="card-body">
            <div class="card-label">Sản Phẩm Đã Bán</div>
            <div class="card-value">{{ activeSummary.productsSold?.toLocaleString('vi-VN') || 0 }}</div>
            <div class="card-hint">Số lượng đã bán ({{ periodLabel }})</div>
          </div>
        </div>

        <div class="summary-card card-cancel">
          <div class="card-icon">❌</div>
          <div class="card-body">
            <div class="card-label">Tỷ Lệ Hủy Đơn</div>
            <div class="card-value">{{ activeSummary.cancellationRate || 0 }}%</div>
            <div class="card-hint">Đã hủy: {{ activeSummary.cancelledOrders || 0 }}/{{ activeSummary.totalOrders || 0 }} đơn ({{ periodLabel }})</div>
          </div>
        </div>
      </div>

      <!-- Monthly Revenue Bar Chart -->
      <div class="chart-section">
        <div class="chart-header">
          <h2 class="chart-title">
            📈 Biểu Đồ Doanh Thu {{ selectedPeriod === 'custom' ? `12 Tháng (Từ ${customFrom})` : `Theo Tháng — Năm ${selectedYear}` }}
          </h2>
        </div>

        <div class="chart-area">
          <div class="chart-bars">
            <div
              v-for="m in summary.monthlyRevenues"
              :key="`${m.year}-${m.month}`"
              class="bar-col"
            >
              <div class="bar-tooltip">
                <div class="tooltip-title">Tháng {{ m.month }}/{{ m.year }}</div>
                <div class="tooltip-value">{{ formatFullPrice(m.revenue) }}</div>
                <div class="tooltip-orders">{{ m.orderCount }} đơn hàng</div>
              </div>
              <div class="bar-wrapper">
                <div
                  class="bar-fill"
                  :style="{ height: barHeightPercent(m.revenue) + '%' }"
                  :class="{ 'bar-empty': !m.revenue }"
                >
                </div>
              </div>
              <div class="bar-label">T{{ m.month }}/{{ String(m.year).slice(2) }}</div>
            </div>
          </div>

          <!-- Y-Axis Labels -->
          <div class="y-axis">
            <span>{{ formatPrice(maxMonthlyRevenue) }}</span>
            <span>{{ formatPrice(maxMonthlyRevenue * 0.75) }}</span>
            <span>{{ formatPrice(maxMonthlyRevenue * 0.5) }}</span>
            <span>{{ formatPrice(maxMonthlyRevenue * 0.25) }}</span>
            <span>0</span>
          </div>
        </div>

        <!-- Chart Summary Row -->
        <div class="chart-bottom">
          <div class="chart-stat">
            <span>📅 Tháng có doanh thu cao nhất:</span>
            <strong>
              {{
                (() => {
                  const top = [...summary.monthlyRevenues].sort((a, b) => b.revenue - a.revenue)[0];
                  return top && top.revenue > 0 ? `Tháng ${top.month}/${top.year} — ${formatFullPrice(top.revenue)}` : 'Chưa có dữ liệu';
                })()
              }}
            </strong>
          </div>
          <div class="chart-stat">
            <span>📊 Doanh thu trung bình/tháng:</span>
            <strong>
              {{
                (() => {
                  const active = summary.monthlyRevenues.filter(m => m.revenue > 0);
                  if (!active.length) return 'Chưa có dữ liệu';
                  const avg = active.reduce((s, m) => s + m.revenue, 0) / active.length;
                  return formatFullPrice(Math.round(avg));
                })()
              }}
            </strong>
          </div>
        </div>
      </div>

      <!-- Single Merged Products Table -->
      <div class="all-products-section" v-if="allProductsWithRank && allProductsWithRank.length">
        <div class="all-products-header">
          <h2 class="section-title">📋 Danh Sách Tất Cả Sản Phẩm</h2>
          <div class="all-products-controls">
            <input 
              type="text" 
              v-model="searchKeyword" 
              placeholder="🔍 Tìm theo tên sản phẩm..." 
              class="search-input"
            />
            <select v-model="sortOrder" class="sort-select">
              <option value="desc">📶 Sắp xếp: Cao -> Thấp</option>
              <option value="asc">📶 Sắp xếp: Thấp -> Cao</option>
            </select>
          </div>
        </div>

        <div class="table-wrapper">
          <table class="product-table">
            <thead>
              <tr>
                <th>Thứ hạng</th>
                <th>Tên Sản Phẩm</th>
                <th>Số Lượng Đã Bán</th>
                <th>Doanh Thu</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in paginatedProducts" :key="p.productId">
                <td>
                  <span :class="['rank-badge', p.originalRank === 1 ? 'rank-gold' : p.originalRank === 2 ? 'rank-silver' : p.originalRank === 3 ? 'rank-bronze' : 'rank-normal']">
                    {{ p.originalRank === 1 ? '🥇' : p.originalRank === 2 ? '🥈' : p.originalRank === 3 ? '🥉' : `#${p.originalRank}` }}
                  </span>
                </td>
                <td 
                  class="product-name-cell clickable-prod-name" 
                  @click="goToProductDetail(p.productId)"
                  title="Bấm để xem trang mua hàng sản phẩm này"
                >
                  {{ p.productName }}
                </td>
                <td class="qty-cell">{{ p.displayQty?.toLocaleString('vi-VN') }} SP</td>
                <td class="revenue-cell">{{ formatFullPrice(p.displayRevenue) }}</td>
              </tr>
              <tr v-if="filteredAndSortedProducts.length === 0">
                <td colspan="4" class="empty-data">Không tìm thấy sản phẩm nào phù hợp.</td>
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
      </div>
    </template>
  </div>
</template>

<style scoped>
.dashboard-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem 5rem;
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2.5rem;
  gap: 1rem;
  flex-wrap: wrap;
}

.page-badge {
  font-size: 0.72rem;
  font-weight: 800;
  color: #6366f1;
  letter-spacing: 1.5px;
  margin-bottom: 0.35rem;
}

.page-title {
  font-size: 2rem;
  font-weight: 900;
  color: #0f172a;
  margin: 0 0 0.3rem;
}

.page-subtitle {
  color: #64748b;
  font-size: 0.9rem;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.period-picker {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: white;
  border: 1px solid #e2e8f0;
  padding: 0.5rem 0.85rem;
  border-radius: 12px;
}

.picker-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #64748b;
}

.header-select {
  border: none;
  background: transparent;
  font-size: 0.85rem;
  font-weight: 700;
  color: #1e293b;
  outline: none;
  cursor: pointer;
}

.custom-range-picker {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: white;
  border: 1.5px solid #6366f1;
  padding: 0.4rem 0.75rem;
  border-radius: 12px;
}

.date-input {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 0.25rem 0.5rem;
  font-size: 0.82rem;
  font-weight: 700;
  color: #1e293b;
  outline: none;
}

.btn-apply-range {
  background: #6366f1;
  color: white;
  border: none;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-apply-range:hover {
  background: #4f46e5;
}

.year-picker {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: white;
  border: 1px solid #e2e8f0;
  padding: 0.6rem 1rem;
  border-radius: 12px;
}

.year-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #64748b;
}

.year-btn {
  background: none;
  border: none;
  padding: 0.3rem 0.7rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.85rem;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.year-btn.active {
  background: #6366f1;
  color: white;
}

/* Loading */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 5rem 1rem;
  gap: 1rem;
  color: #64748b;
}

.spinner-lg {
  width: 48px;
  height: 48px;
  border: 4px solid #e2e8f0;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* Summary Cards */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
  margin-bottom: 2.5rem;
}

@media (max-width: 1024px) {
  .summary-cards { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .summary-cards { grid-template-columns: 1fr; }
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  padding: 1.75rem 1.5rem;
  border-radius: 20px;
  border: 1px solid transparent;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}

.card-revenue {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: white;
}

.card-orders {
  background: linear-gradient(135deg, #0891b2 0%, #0e7490 100%);
  color: white;
}

.card-products {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  color: white;
}

.card-cancel {
  background: linear-gradient(135deg, #e11d48 0%, #be123c 100%);
  color: white;
}

.card-icon {
  font-size: 2.5rem;
  line-height: 1;
}

.card-label {
  font-size: 0.82rem;
  font-weight: 700;
  opacity: 0.85;
  margin-bottom: 0.25rem;
}

.card-value {
  font-size: 1.75rem;
  font-weight: 900;
  line-height: 1.1;
  margin-bottom: 0.2rem;
}

.card-hint {
  font-size: 0.72rem;
  opacity: 0.7;
}

/* Bar Chart Section */
.chart-section {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 2rem;
  margin-bottom: 2.5rem;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
}

.chart-header {
  margin-bottom: 2rem;
}

.chart-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: #0f172a;
}

.chart-area {
  display: flex;
  align-items: flex-end;
  gap: 0.5rem;
  height: 260px;
  position: relative;
  padding-bottom: 2rem;
  border-bottom: 2px solid #f1f5f9;
}

.chart-bars {
  flex: 1;
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
  height: 100%;
}

.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  position: relative;
  gap: 0.25rem;
}

/* Tooltip */
.bar-tooltip {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  background: #0f172a;
  color: white;
  padding: 0.5rem 0.75rem;
  border-radius: 8px;
  font-size: 0.72rem;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s;
  z-index: 10;
}

.bar-tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: #0f172a;
}

.tooltip-title { font-weight: 700; font-size: 0.75rem; margin-bottom: 0.15rem; }
.tooltip-value { color: #a5f3fc; font-weight: 800; }
.tooltip-orders { color: #94a3b8; font-size: 0.68rem; }

.bar-col:hover .bar-tooltip { opacity: 1; }

.bar-wrapper {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
}

.bar-fill {
  width: 100%;
  min-height: 4px;
  background: linear-gradient(180deg, #818cf8 0%, #4f46e5 100%);
  border-radius: 6px 6px 0 0;
  transition: height 0.6s ease;
}

.bar-fill.bar-empty {
  background: #e2e8f0;
}

.bar-col:hover .bar-fill:not(.bar-empty) {
  background: linear-gradient(180deg, #a5b4fc 0%, #6366f1 100%);
}

.bar-label {
  font-size: 0.72rem;
  font-weight: 700;
  color: #64748b;
}

/* Y Axis */
.y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: calc(100% - 2rem);
  text-align: right;
  font-size: 0.68rem;
  color: #94a3b8;
  font-weight: 700;
  min-width: 70px;
  padding-bottom: 0.25rem;
}

/* Chart Bottom Stats */
.chart-bottom {
  display: flex;
  gap: 2rem;
  flex-wrap: wrap;
  margin-top: 1.25rem;
  padding-top: 1.25rem;
  border-top: 1px solid #f1f5f9;
}

.chart-stat {
  font-size: 0.85rem;
  color: #475569;
}

.chart-stat strong {
  color: #0f172a;
  font-weight: 800;
  margin-left: 0.4rem;
}

/* Top Products Table */
.top-products-section {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 2rem;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
}

.section-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 1.5rem;
}

.table-wrapper {
  overflow-x: auto;
}

.product-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.product-table th {
  background: #f8fafc;
  padding: 0.85rem 1rem;
  text-align: left;
  font-size: 0.8rem;
  font-weight: 800;
  color: #64748b;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  border-bottom: 1px solid #e2e8f0;
}

.product-table td {
  padding: 1rem;
  border-bottom: 1px solid #f8fafc;
  vertical-align: middle;
}

.product-table tr:last-child td {
  border-bottom: none;
}

.product-table tr:hover td {
  background: #fafbff;
}

.rank-badge {
  font-size: 1.1rem;
}

.rank-normal {
  font-weight: 800;
  color: #64748b;
}

.product-name-cell {
  font-weight: 700;
  color: #0f172a;
}

.qty-cell {
  font-weight: 800;
  color: #0891b2;
}

.revenue-cell {
  font-weight: 900;
  color: #4f46e5;
}

.empty-data {
  text-align: center;
  padding: 2.5rem;
  background: #f8fafc;
  border-radius: 12px;
  color: #64748b;
}

/* All Products Section */
.all-products-section {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 2rem;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
  margin-top: 2.5rem;
}

.all-products-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.all-products-header .section-title {
  margin-bottom: 0;
}

.all-products-controls {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.search-input {
  padding: 0.6rem 1rem;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.9rem;
  min-width: 250px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #6366f1;
}

.sort-select {
  padding: 0.6rem 1rem;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.9rem;
  outline: none;
  background-color: white;
  cursor: pointer;
}

@media (max-width: 768px) {
  .all-products-header {
    flex-direction: column;
    align-items: stretch;
  }
  .all-products-controls {
    flex-direction: column;
    align-items: stretch;
  }
}

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.4rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid #f1f5f9;
}

.page-btn {
  background: white;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 0.4rem 0.8rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.page-btn:hover:not(:disabled) {
  border-color: #6366f1;
  color: #6366f1;
  background: #f5f3ff;
}

.page-btn.active {
  background: #6366f1;
  border-color: #6366f1;
  color: white;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.clickable-prod-name {
  cursor: pointer;
  color: #0f172a;
  font-weight: 600;
  transition: all 0.15s ease;
}

.clickable-prod-name:hover {
  color: #6366f1;
  font-weight: 800;
  text-decoration: underline;
}
</style>

<!-- Feature Implementation: làm trang thống kê doanh thu và báo cáo admin -->
