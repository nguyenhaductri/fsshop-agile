<script setup>
import { ref, onMounted, watch } from 'vue';
import { publicProductApi } from '../services/api';

const emit = defineEmits(['navigate', 'view-product']);

const products = ref([]);
const categories = ref([]);
const totalPages = ref(1);
const currentPage = ref(0);
const loading = ref(false);

// Filter states
const searchQuery = ref('');
const suggestions = ref([]);
const showSuggestions = ref(false);
const selectedCategory = ref('');
const selectedSize = ref('');
const selectedColor = ref('');
const minPrice = ref(null);
const maxPrice = ref(null);
const minRating = ref(0);
const sortBy = ref('newest');

onMounted(async () => {
  await loadCategories();
  await fetchProducts();
});

async function loadCategories() {
  try {
    const res = await publicProductApi.getCategories();
    categories.value = res.data || [];
  } catch (err) {
    console.error('Lỗi tải danh mục:', err);
  }
}

async function fetchProducts(page = 0) {
  loading.value = true;
  currentPage.value = page;
  try {
    const cleanMinPrice = minPrice.value && Number(minPrice.value) > 0 ? Number(minPrice.value) : null;
    const cleanMaxPrice = maxPrice.value && Number(maxPrice.value) > 0 ? Number(maxPrice.value) : null;

    const params = {
      page,
      size: 9,
      keyword: searchQuery.value ? searchQuery.value.trim() : null,
      categoryId: selectedCategory.value ? Number(selectedCategory.value) : null,
      sizeParam: selectedSize.value || null,
      color: selectedColor.value || null,
      minPrice: cleanMinPrice,
      maxPrice: cleanMaxPrice,
      minRating: minRating.value && Number(minRating.value) > 0 ? Number(minRating.value) : null,
      sortBy: sortBy.value,
    };
    const res = await publicProductApi.getProducts(params);
    products.value = res.data.content || [];
    totalPages.value = res.data.totalPages || 1;
  } catch (err) {
    console.error('Lỗi tải danh sách sản phẩm:', err);
  } finally {
    loading.value = false;
  }
}

// Auto-suggest logic & Grid Search (US11 & US12)
watch(searchQuery, async (newQuery) => {
  fetchProducts(0); // Tự động lọc lại Grid sản phẩm theo từ khóa nhập vào

  if (!newQuery || newQuery.trim().length < 2) {
    suggestions.value = [];
    showSuggestions.value = false;
    return;
  }
  try {
    const res = await publicProductApi.searchProducts(newQuery.trim());
    suggestions.value = res.data || [];
    showSuggestions.value = suggestions.value.length > 0;
  } catch (err) {
    suggestions.value = [];
  }
});

function selectSuggestion(prod) {
  showSuggestions.value = false;
  searchQuery.value = '';
  emit('view-product', prod.id);
}

function toggleStarFilter(star) {
  if (minRating.value === star) {
    minRating.value = 0;
  } else {
    minRating.value = star;
  }
  fetchProducts(0);
}

function resetFilters() {
  selectedCategory.value = '';
  selectedSize.value = '';
  selectedColor.value = '';
  minPrice.value = null;
  maxPrice.value = null;
  minRating.value = 0;
  sortBy.value = 'newest';
  searchQuery.value = '';
  fetchProducts(0);
}

function formatPrice(val) {
  if (!val) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}

function calcDiscount(price, salePrice) {
  if (!price || !salePrice || salePrice >= price) return 0;
  return Math.round(((price - salePrice) / price) * 100);
}
</script>

<template>
  <div class="catalog-page">
    <!-- Search Bar with Auto-suggest (US11 & US12) -->
    <div class="search-header">
      <div class="search-wrapper">
        <div class="search-input-box">
          <span class="search-icon">🔍</span>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Tìm kiếm theo tên sản phẩm hoặc mã SKU (Ví dụ: Áo thun, SP001)..."
            @focus="showSuggestions = suggestions.length > 0"
          />
          <button v-if="searchQuery" class="btn-clear" @click="searchQuery = ''; showSuggestions = false">✖</button>
        </div>

        <!-- Auto-suggest Dropdown Panel -->
        <div v-if="showSuggestions" class="suggestions-panel">
          <div
            v-for="item in suggestions"
            :key="item.id"
            class="suggestion-item"
            @click="selectSuggestion(item)"
          >
            <img :src="item.thumbnailUrl || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100'" class="sug-img" />
            <div class="sug-info">
              <div class="sug-name">{{ item.name }}</div>
              <div class="sug-meta">SKU: <code>{{ item.sku }}</code> | <span class="sug-price">{{ formatPrice(item.salePrice || item.price) }}</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="catalog-container">
      <!-- Sidebar Bộ Lọc (US10) -->
      <aside class="filter-sidebar">
        <div class="sidebar-header">
          <h3> Bộ Lọc Sản Phẩm</h3>
          <button class="btn-reset" @click="resetFilters">Đặt lại</button>
        </div>

        <!-- Danh mục -->
        <div class="filter-group">
          <label>Danh Mục Thời Trang</label>
          <select v-model="selectedCategory" @change="fetchProducts(0)">
            <option value="">Tất cả danh mục</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
        </div>

        <!-- Kích cỡ (Size) -->
        <div class="filter-group">
          <label>Kích Cỡ (Size)</label>
          <div class="size-options">
            <button
              v-for="s in ['', 'S', 'M', 'L', 'XL', 'XXL', '31']"
              :key="s"
              :class="['size-btn', selectedSize === s ? 'active' : '']"
              @click="selectedSize = s; fetchProducts(0)"
            >
              {{ s || 'Tất cả' }}
            </button>
          </div>
        </div>

        <!-- Màu sắc -->
        <div class="filter-group">
          <label>Màu Sắc</label>
          <div class="color-options">
            <button
              v-for="c in ['', 'Đen', 'Trắng', 'Xanh', 'Navy', 'Xám']"
              :key="c"
              :class="['color-btn', selectedColor === c ? 'active' : '']"
              @click="selectedColor = c; fetchProducts(0)"
            >
              {{ c || 'Tất cả' }}
            </button>
          </div>
        </div>

        <!-- Khoảng giá -->
        <div class="filter-group">
          <label>Khoảng Giá (VND)</label>
          <div class="price-inputs">
            <input v-model.number="minPrice" type="number" placeholder="Từ..." @change="fetchProducts(0)" />
            <span>-</span>
            <input v-model.number="maxPrice" type="number" placeholder="Đến..." @change="fetchProducts(0)" />
          </div>
        </div>

        <!-- Lọc theo số sao đánh giá (Thanh 5 sao gọn gàng tương tác) -->
        <div class="filter-group">
          <label>Đánh Giá (Mức ⭐)</label>
          <div class="star-rating-filter-box">
            <div class="star-bar-picker">
              <span
                v-for="star in 5"
                :key="star"
                :class="['star-pick-icon', star <= minRating ? 'active' : '']"
                @click="toggleStarFilter(star)"
              >
                ★
              </span>
            </div>
            <div class="rating-filter-status">
              <span v-if="minRating > 0" class="status-active-text">Từ {{ minRating }}.0 ⭐ trở lên</span>
              <span v-else class="status-all-text">Tất cả sản phẩm</span>
              <button v-if="minRating > 0" class="btn-clear-rating" @click="minRating = 0; fetchProducts(0)">✖ Xóa lọc</button>
            </div>
          </div>
        </div>

        <!-- Sắp xếp -->
        <div class="filter-group">
          <label>Sắp Xếp Theo</label>
          <select v-model="sortBy" @change="fetchProducts(0)">
            <option value="newest">Mới nhất</option>
            <option value="priceAsc">Giá tăng dần</option>
            <option value="priceDesc">Giá giảm dần</option>
          </select>
        </div>
      </aside>

      <!-- Main Product Grid (US09) -->
      <main class="product-grid-section">
        <div v-if="loading" class="loading-state">
          <span class="spinner"></span> Đang tải danh sách sản phẩm...
        </div>

        <div v-else-if="!products.length" class="empty-state">
          <div class="empty-icon">🛍️</div>
          <h3>Không tìm thấy sản phẩm nào!</h3>
          <p>Thử bỏ bớt bộ lọc hoặc gõ từ khóa tìm kiếm khác.</p>
          <button class="btn-shop-all" @click="resetFilters">Xem tất cả sản phẩm</button>
        </div>

        <div v-else class="product-grid">
          <div
            v-for="prod in products"
            :key="prod.id"
            class="product-card"
            @click="emit('view-product', prod.id)"
          >
            <div class="card-image-wrapper">
              <img :src="prod.thumbnailUrl || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500'" class="card-img" />
              <span v-if="calcDiscount(prod.price, prod.salePrice) > 0" class="badge-discount">
                -{{ calcDiscount(prod.price, prod.salePrice) }}%
              </span>
            </div>

            <div class="card-content">
              <div class="card-meta-row">
                <span class="card-cat">{{ prod.categoryName || 'FS Collection' }}</span>
                <span class="card-rating-badge">
                  ⭐ {{ prod.averageRating ? prod.averageRating.toFixed(1) : '0.0' }}
                  <span class="review-count">({{ prod.reviewCount || 0 }})</span>
                </span>
              </div>
              <h4 class="card-title">{{ prod.name }}</h4>

              <div class="card-price-row">
                <span class="card-sale-price">{{ formatPrice(prod.salePrice || prod.price) }}</span>
                <span v-if="prod.salePrice && prod.salePrice < prod.price" class="card-old-price">
                  {{ formatPrice(prod.price) }}
                </span>
              </div>

              <div class="card-footer">
                <button class="btn-view-detail">Xem Chi Tiết ➔</button>
              </div>
            </div>
          </div>
        </div>

        <!-- Phân trang (Pagination) -->
        <div v-if="totalPages > 1" class="pagination">
          <button
            class="page-btn"
            :disabled="currentPage === 0"
            @click="fetchProducts(currentPage - 1)"
          >
            ◄ Trẻ trước
          </button>
          <span class="page-info">Trang {{ currentPage + 1 }} / {{ totalPages }}</span>
          <button
            class="page-btn"
            :disabled="currentPage >= totalPages - 1"
            @click="fetchProducts(currentPage + 1)"
          >
            Trang sau ►
          </button>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.catalog-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1.5rem 4rem 1.5rem;
}

/* Search Header */
.search-header {
  margin-bottom: 2rem;
  display: flex;
  justify-content: center;
}

.search-wrapper {
  position: relative;
  width: 100%;
  max-width: 680px;
}

.search-input-box {
  display: flex;
  align-items: center;
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 16px;
  padding: 0.6rem 1.25rem;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.04);
  transition: border-color 0.2s ease;
}

.search-input-box:focus-within {
  border-color: #0284c7;
}

.search-icon {
  font-size: 1.2rem;
  margin-right: 0.75rem;
}

.search-input-box input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 1rem;
  color: #0f172a;
  background: transparent;
}

.btn-clear {
  background: none;
  border: none;
  cursor: pointer;
  color: #94a3b8;
}

/* Auto-suggest dropdown */
.suggestions-panel {
  position: absolute;
  top: 110%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  z-index: 500;
  overflow: hidden;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.25rem;
  cursor: pointer;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.2s ease;
}

.suggestion-item:hover {
  background: #f8fafc;
}

.sug-img {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  object-fit: cover;
}

.sug-name {
  font-weight: 700;
  font-size: 0.9rem;
  color: #0f172a;
}

.sug-meta {
  font-size: 0.8rem;
  color: #64748b;
}

.sug-price {
  color: #f97316;
  font-weight: 800;
}

/* Catalog Container & Sidebar */
.catalog-container {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 2rem;
}

@media (max-width: 850px) {
  .catalog-container {
    grid-template-columns: 1fr;
  }
}

.filter-sidebar {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 1.5rem;
  height: fit-content;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #f1f5f9;
}

.sidebar-header h3 {
  font-size: 1.1rem;
  font-weight: 800;
  color: #0f172a;
}

.btn-reset {
  background: none;
  border: none;
  color: #0284c7;
  font-weight: 700;
  font-size: 0.8rem;
  cursor: pointer;
}

.filter-group {
  margin-bottom: 1.5rem;
}

.filter-group label {
  display: block;
  font-size: 0.85rem;
  font-weight: 700;
  color: #334155;
  margin-bottom: 0.5rem;
}

.filter-group select {
  width: 100%;
  padding: 0.6rem 0.8rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  font-size: 0.9rem;
  outline: none;
  background: #f8fafc;
}

.size-options, .color-options {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.size-btn, .color-btn {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  color: #475569;
  padding: 0.35rem 0.65rem;
  border-radius: 8px;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.size-btn.active, .color-btn.active {
  background: #0284c7;
  color: white;
  border-color: #0284c7;
}

.price-inputs {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.price-inputs input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.85rem;
}

/* Product Grid */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1.5rem;
}

.product-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  display: flex;
  flex-direction: column;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.08);
}

.card-image-wrapper {
  position: relative;
  width: 100%;
  height: 240px;
  overflow: hidden;
  background: #f8fafc;
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product-card:hover .card-img {
  transform: scale(1.05);
}

.badge-discount {
  position: absolute;
  top: 0.75rem;
  left: 0.75rem;
  background: #ef4444;
  color: white;
  font-weight: 800;
  font-size: 0.75rem;
  padding: 0.2rem 0.6rem;
  border-radius: 8px;
}

.card-content {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  flex: 1;
  justify-content: space-between;
}

.card-cat {
  font-size: 0.75rem;
  font-weight: 700;
  color: #0284c7;
  margin-bottom: 0.25rem;
}

.card-title {
  font-size: 1rem;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 0.75rem;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-price-row {
  display: flex;
  align-items: baseline;
  gap: 0.6rem;
  margin-bottom: 1rem;
}

.card-sale-price {
  font-size: 1.15rem;
  font-weight: 800;
  color: #f97316;
}

.card-old-price {
  font-size: 0.85rem;
  color: #94a3b8;
  text-decoration: line-through;
}

.btn-view-detail {
  width: 100%;
  background: #f1f5f9;
  color: #0f172a;
  border: none;
  padding: 0.65rem;
  border-radius: 10px;
  font-weight: 800;
  font-size: 0.85rem;
  cursor: pointer;
  transition: background 0.2s ease;
}

.product-card:hover .btn-view-detail {
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: white;
}

/* Pagination */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1.5rem;
  margin-top: 3rem;
}

.page-btn {
  background: white;
  border: 1.5px solid #cbd5e1;
  color: #0f172a;
  padding: 0.6rem 1.25rem;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-weight: 700;
  color: #475569;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 4rem 1rem;
  background: white;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.btn-shop-all {
  margin-top: 1rem;
  background: #0284c7;
  color: white;
  border: none;
  padding: 0.6rem 1.25rem;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
}

.star-rating-filter-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 0.75rem 0.85rem;
}

.star-bar-picker {
  display: flex;
  gap: 0.35rem;
  margin-bottom: 0.5rem;
}

.star-pick-icon {
  font-size: 1.5rem;
  color: #cbd5e1;
  cursor: pointer;
  transition: transform 0.15s ease, color 0.15s ease;
  user-select: none;
}

.star-pick-icon:hover {
  transform: scale(1.25);
  color: #f59e0b;
}

.star-pick-icon.active {
  color: #f59e0b;
  text-shadow: 0 0 8px rgba(245, 158, 11, 0.4);
}

.rating-filter-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.78rem;
  font-weight: 700;
}

.status-active-text {
  color: #d97706;
}

.status-all-text {
  color: #94a3b8;
}

.btn-clear-rating {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
  padding: 0.15rem 0.45rem;
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 800;
  cursor: pointer;
}

.btn-clear-rating:hover {
  background: #fee2e2;
}

.card-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.35rem;
}

.card-rating-badge {
  font-size: 0.75rem;
  font-weight: 800;
  color: #d97706;
  background: #fef3c7;
  padding: 0.15rem 0.45rem;
  border-radius: 6px;
}

.review-count {
  color: #78350f;
  font-weight: 600;
}
</style>

<!-- Feature Implementation: ui grid, thẻ sp, phân trang -->

<!-- Feature Implementation: ui sidebar lọc (size, màu, giá) -->

<!-- Stage 1: Product Grid & Pagination Controls -->