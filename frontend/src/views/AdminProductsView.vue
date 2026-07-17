<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { adminProductApi, adminCategoryApi, uploadApi } from '../services/api';
import { authState } from '../state/authStore';

const emit = defineEmits(['navigate']);

const prodFileInputRef = ref(null);
const uploadingProdImage = ref(false);

async function handleProdImageUpload(event) {
  const file = event.target.files[0];
  if (!file) return;

  uploadingProdImage.value = true;
  try {
    const res = await uploadApi.uploadFile(file);
    formImageUrl.value = res.data.fileUrl;
  } catch (err) {
    alert(err.message || 'Lỗi tải ảnh sản phẩm!');
  } finally {
    uploadingProdImage.value = false;
  }
}

const products = ref([]);
const categories = ref([]);
const inventory = ref({ totalStockQuantity: 0, lowStockCount: 0, lowStockItems: [] });
const loading = ref(false);
const message = ref('');
const isError = ref(false);

// Inventory Quick Filter
const stockFilterStatus = ref('all'); // 'all', 'low', 'out'
const adminSearchQuery = ref('');

// Product Modal state
const showModal = ref(false);
const isEditing = ref(false);
const editingProductId = ref(null);

// Product Form state
const formSku = ref('');
const formName = ref('');
const formDescription = ref('');
const formPrice = ref(0);
const formSalePrice = ref(0);
const formCategoryId = ref('');
const formImageUrl = ref('');
const formVariants = ref([
  { size: 'M', color: 'Đen', stockQuantity: 10, skuVariant: '' },
  { size: 'L', color: 'Đen', stockQuantity: 5, skuVariant: '' }
]);

// Category Modal State
const showCategoryModal = ref(false);
const categoryNameInput = ref('');
const categoryDescInput = ref('');
const editingCatId = ref(null);

onMounted(async () => {
  if (!authState.user || (authState.user.role !== 'ROLE_ADMIN' && authState.user.role !== 'ROLE_OWNER')) {
    emit('navigate', 'home');
    return;
  }
  await loadData();
});

async function loadData() {
  loading.value = true;
  try {
    const [prodRes, invRes, catRes] = await Promise.all([
      adminProductApi.getProducts(0, 100),
      adminProductApi.getInventorySummary(),
      adminCategoryApi.getCategories()
    ]);
    products.value = prodRes.data.content || [];
    inventory.value = invRes.data || {};
    categories.value = catRes.data || [];
  } catch (err) {
    message.value = err.message || 'Lỗi tải dữ liệu kho hàng!';
    isError.value = true;
  } finally {
    loading.value = false;
  }
}

// Filtered products list for Quick Stock Filter & Admin Search
const filteredProductsList = computed(() => {
  let list = products.value;

  if (stockFilterStatus.value === 'low') {
    list = list.filter(p => 
      p.totalStock < 5 || (p.variants && p.variants.some(v => v.stockQuantity < 5))
    );
  } else if (stockFilterStatus.value === 'out') {
    list = list.filter(p => 
      p.totalStock === 0 || (p.variants && p.variants.some(v => v.stockQuantity === 0))
    );
  }

  if (adminSearchQuery.value && adminSearchQuery.value.trim()) {
    const q = adminSearchQuery.value.trim().toLowerCase();
    list = list.filter(p => 
      (p.name && p.name.toLowerCase().includes(q)) ||
      (p.sku && p.sku.toLowerCase().includes(q)) ||
      (p.categoryName && p.categoryName.toLowerCase().includes(q))
    );
  }

  return list;
});

// Pagination features (10 items per page)
const currentPage = ref(1);
const pageSize = 10;

const totalPages = computed(() => {
  return Math.ceil(filteredProductsList.value.length / pageSize) || 1;
});

const paginatedProductsList = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredProductsList.value.slice(start, start + pageSize);
});

watch([stockFilterStatus, adminSearchQuery], () => {
  currentPage.value = 1;
});

function openCreateModal() {
  isEditing.value = false;
  editingProductId.value = null;
  formSku.value = '';
  formName.value = '';
  formDescription.value = '';
  formPrice.value = 250000;
  formSalePrice.value = 199000;
  formCategoryId.value = categories.value.length ? categories.value[0].id : '';
  formImageUrl.value = 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500';
  formVariants.value = [
    { size: 'M', color: 'Đen', stockQuantity: 10, skuVariant: '' },
    { size: 'L', color: 'Trắng', stockQuantity: 3, skuVariant: '' }
  ];
  showModal.value = true;
}

function openEditModal(prod) {
  isEditing.value = true;
  editingProductId.value = prod.id;
  formSku.value = prod.sku;
  formName.value = prod.name;
  formDescription.value = prod.description || '';
  formPrice.value = prod.price;
  formSalePrice.value = prod.salePrice || 0;
  formCategoryId.value = prod.categoryId || '';
  formImageUrl.value = prod.thumbnailUrl || (prod.imageUrls && prod.imageUrls[0]) || '';
  formVariants.value = prod.variants && prod.variants.length
    ? prod.variants.map(v => ({ size: v.size, color: v.color, stockQuantity: v.stockQuantity, skuVariant: v.skuVariant }))
    : [{ size: 'M', color: 'Đen', stockQuantity: 5, skuVariant: '' }];
  showModal.value = true;
}

function addVariantRow() {
  formVariants.value.push({ size: 'L', color: 'Xanh', stockQuantity: 10, skuVariant: '' });
}

function removeVariantRow(index) {
  if (formVariants.value.length > 1) {
    formVariants.value.splice(index, 1);
  }
}

async function handleSaveProduct() {
  if (!formName.value || !formPrice.value) {
    alert('Vui lòng điền các trường bắt buộc (Tên sản phẩm, Giá niêm yết)!');
    return;
  }

  const payload = {
    sku: formSku.value,
    name: formName.value,
    description: formDescription.value,
    price: formPrice.value,
    salePrice: formSalePrice.value > 0 ? formSalePrice.value : null,
    categoryId: formCategoryId.value ? Number(formCategoryId.value) : null,
    imageUrls: formImageUrl.value ? [formImageUrl.value] : [],
    variants: formVariants.value
  };

  try {
    if (isEditing.value) {
      await adminProductApi.updateProduct(editingProductId.value, payload);
      message.value = 'Cập nhật sản phẩm thành công!';
    } else {
      await adminProductApi.createProduct(payload);
      message.value = 'Thêm mới sản phẩm thành công!';
    }
    isError.value = false;
    showModal.value = false;
    await loadData();
  } catch (err) {
    alert(err.message || 'Lưu sản phẩm thất bại!');
  }
}

async function handleDeleteProduct(prod) {
  if (confirm(`Bạn có chắc chắn muốn xóa sản phẩm "${prod.name}" (Xóa mềm status=0)?`)) {
    try {
      await adminProductApi.deleteProduct(prod.id);
      message.value = `Đã xóa mềm sản phẩm ${prod.name}`;
      isError.value = false;
      await loadData();
    } catch (err) {
      alert(err.message || 'Xóa thất bại!');
    }
  }
}

// Category CRUD functions
async function handleAddCategory() {
  if (!categoryNameInput.value) {
    alert('Vui lòng nhập tên danh mục!');
    return;
  }
  try {
    if (editingCatId.value) {
      await adminCategoryApi.updateCategory(editingCatId.value, {
        name: categoryNameInput.value,
        description: categoryDescInput.value
      });
      message.value = 'Đã cập nhật danh mục!';
    } else {
      await adminCategoryApi.createCategory({
        name: categoryNameInput.value,
        description: categoryDescInput.value
      });
      message.value = 'Đã thêm danh mục mới!';
    }
    categoryNameInput.value = '';
    categoryDescInput.value = '';
    editingCatId.value = null;
    await loadData();
  } catch (err) {
    alert(err.message || 'Thao tác danh mục thất bại!');
  }
}

function startEditCategory(cat) {
  editingCatId.value = cat.id;
  categoryNameInput.value = cat.name;
  categoryDescInput.value = cat.description || '';
}

async function handleDeleteCategory(cat) {
  if (confirm(`Bạn có chắc chắn muốn xóa danh mục "${cat.name}"?`)) {
    try {
      await adminCategoryApi.deleteCategory(cat.id);
      await loadData();
    } catch (err) {
      alert(err.message || 'Xóa danh mục thất bại!');
    }
  }
}

function formatPrice(val) {
  if (!val) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}

function goToProductDetail(productId) {
  if (productId) {
    emit('navigate', 'product-detail', productId);
  }
}
</script>

<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <span class="badge-admin">QUẢN TRỊ KHO (ADMIN)</span>
        <h1>Quản Lý Sản Phẩm &amp; Tồn Kho</h1>
        <p>Quản lý danh mục sản phẩm, biến thể Size/Màu sắc và cảnh báo tồn kho</p>
      </div>

      <div class="header-actions">
        <button class="btn-category" @click="showCategoryModal = true">
          📂 QUẢN LÝ DANH MỤC
        </button>
        <button class="btn-create" @click="openCreateModal">
          ➕ THÊM SẢN PHẨM MỚI
        </button>
      </div>
    </div>

    <!-- Alert message -->
    <div v-if="message" :class="['alert', isError ? 'alert-error' : 'alert-success']">
      {{ isError ? '⚠️' : '✅' }} {{ message }}
    </div>

    <!-- Inventory Dashboard Summary (US08) -->
    <div class="inventory-summary-cards">
      <div class="stat-card" @click="stockFilterStatus = 'all'">
        <div class="stat-icon">📦</div>
        <div>
          <div class="stat-value">{{ inventory.totalStockQuantity || 0 }}</div>
          <div class="stat-label">TỔNG SỐ LƯỢNG TỒN KHO</div>
        </div>
      </div>

      <div class="stat-card warning" @click="stockFilterStatus = 'low'" v-if="inventory.lowStockCount > 0">
        <div class="stat-icon">⚠️</div>
        <div>
          <div class="stat-value text-red">{{ inventory.lowStockCount }}</div>
          <div class="stat-label">BIẾN THỂ SẮP HẾT HÀNG (&lt; 5 SP)</div>
        </div>
      </div>

      <div class="stat-card success" v-else>
        <div class="stat-icon">✅</div>
        <div>
          <div class="stat-value text-green">ĐỦ HÀNG</div>
          <div class="stat-label">KHÔNG CÓ CẢNH BÁO THIẾU KHO</div>
        </div>
      </div>
    </div>

    <!-- Low Stock Alert Banner (US08) -->
    <div v-if="inventory.lowStockItems && inventory.lowStockItems.length > 0" class="low-stock-alert">
      <h4>🚨 CẢNH BÁO TỒN KHO DƯỚI 5 SẢN PHẨM:</h4>
      <div class="low-stock-chips">
        <div v-for="item in inventory.lowStockItems" :key="item.variantId" class="chip-warning">
          <strong>{{ item.productName }}</strong> [{{ item.size }} - {{ item.color }}]: 
          <span class="stock-badge">{{ item.stockQuantity }} SP còn lại</span>
        </div>
      </div>
    </div>

    <!-- Table Header & Quick Inventory Status Filter Pills -->
    <div class="table-filter-bar">
      <h3 class="table-title">Danh Sách Sản Phẩm Trong Kho</h3>

      <div class="admin-search-box">
        <span class="search-icon">🔍</span>
        <input v-model="adminSearchQuery" type="text" placeholder="Tìm tên sản phẩm, mã SKU..." />
        <button v-if="adminSearchQuery" class="btn-clear-search" @click="adminSearchQuery = ''">✖</button>
      </div>

      <div class="stock-filter-pills">
        <button
          :class="['pill-btn', stockFilterStatus === 'all' ? 'active' : '']"
          @click="stockFilterStatus = 'all'"
        >
          Tất cả sản phẩm ({{ products.length }})
        </button>
        <button
          :class="['pill-btn pill-warning', stockFilterStatus === 'low' ? 'active' : '']"
          @click="stockFilterStatus = 'low'"
        >
          ⚠️ Sắp hết hàng (&lt; 5 SP)
        </button>
        <button
          :class="['pill-btn pill-danger', stockFilterStatus === 'out' ? 'active' : '']"
          @click="stockFilterStatus = 'out'"
        >
          ❌ Hết hàng (0 SP)
        </button>
      </div>
    </div>

    <!-- Product Table (US05 - US07) -->
    <div class="table-container">
      <table class="product-table">
        <thead>
          <tr>
            <th>ẢNH</th>
            <th>SKU</th>
            <th>TÊN SẢN PHẨM</th>
            <th>DANH MỤC</th>
            <th>GIÁ NÊM YẾT</th>
            <th>GIÁ SALE</th>
            <th>TỔNG TỒN</th>
            <th>THAO TÁC</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="text-center py-4">Đang tải dữ liệu sản phẩm...</td>
          </tr>
          <tr v-else-if="!filteredProductsList.length">
            <td colspan="8" class="text-center py-4">Không có sản phẩm nào phù hợp với bộ lọc hiện tại!</td>
          </tr>
          <tr v-for="prod in paginatedProductsList" :key="prod.id">
            <td>
              <img 
                :src="prod.thumbnailUrl || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100'" 
                class="thumb-img clickable-thumb" 
                @click="goToProductDetail(prod.id)"
                title="Bấm để xem trang mua hàng"
              />
            </td>
            <td><code>{{ prod.sku }}</code></td>
            <td 
              class="clickable-prod-name" 
              @click="goToProductDetail(prod.id)"
              title="Bấm để xem trang mua hàng"
            >
              {{ prod.name }}
            </td>
            <td><span class="cat-tag">{{ prod.categoryName || 'Chưa phân loại' }}</span></td>
            <td class="text-gray">{{ formatPrice(prod.price) }}</td>
            <td class="text-orange font-bold">{{ formatPrice(prod.salePrice || prod.price) }}</td>
            <td>
              <span :class="['stock-tag', prod.totalStock < 5 ? 'stock-low' : 'stock-ok']">
                {{ prod.totalStock }} SP
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <button class="btn-edit" @click="openEditModal(prod)">✏️ Sửa</button>
                <button class="btn-delete" @click="handleDeleteProduct(prod)">🗑️ Xóa</button>
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

    <!-- Modal Form Thêm/Sửa Sản Phẩm (US05 & US06) -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-card">
        <div class="modal-header">
          <h3>{{ isEditing ? '✏️ Chỉnh Sửa Sản Phẩm' : '➕ Thêm Sản Phẩm Mới' }}</h3>
          <button class="btn-close" @click="showModal = false">✖</button>
        </div>

        <form @submit.prevent="handleSaveProduct" class="modal-form">
          <div class="form-grid">
            <div class="form-group">
              <label>Mã SKU (Tự động sinh nếu bỏ trống)</label>
              <input v-model="formSku" type="text" placeholder="Để trống tự sinh: SP001, SP002..." :disabled="isEditing" />
            </div>
            <div class="form-group">
              <label>Danh mục sản phẩm</label>
              <select v-model="formCategoryId">
                <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label>Tên Sản Phẩm *</label>
            <input v-model="formName" type="text" placeholder="Áo Thun Oversize Cotton..." required />
          </div>

          <div class="form-grid">
            <div class="form-group">
              <label>Giá Niêm Yết (VND) *</label>
              <input v-model.number="formPrice" type="number" step="any" min="0" required />
            </div>
            <div class="form-group">
              <label>Giá Khuyến Mãi (VND)</label>
              <input v-model.number="formSalePrice" type="number" step="any" min="0" />
            </div>
          </div>

          <div class="form-group">
            <label>Hình Ảnh Sản Phẩm</label>
            <div class="file-upload-input-group">
              <input v-model="formImageUrl" type="text" placeholder="Dán link ảnh hoặc tải file từ máy..." />
              <input 
                type="file" 
                ref="prodFileInputRef" 
                accept="image/*" 
                style="display: none" 
                @change="handleProdImageUpload" 
              />
              <button 
                type="button" 
                class="btn-upload-file" 
                @click="prodFileInputRef.click()" 
                :disabled="uploadingProdImage"
              >
                {{ uploadingProdImage ? '⏳ Đang tải...' : '📁 Chọn Ảnh Từ Máy' }}
              </button>
            </div>
            <div v-if="formImageUrl" class="img-preview-box">
              <img :src="formImageUrl" alt="Preview" class="img-preview" />
            </div>
          </div>

          <div class="form-group">
            <label>Mô tả chi tiết sản phẩm</label>
            <textarea v-model="formDescription" rows="3" placeholder="Chất liệu cotton 100% thoáng mát..."></textarea>
          </div>

          <!-- Section Biến thể Size / Màu sắc / Tồn kho -->
          <div class="variants-section">
            <div class="variant-header">
              <label class="font-bold">Biến Thể Sản Phẩm (Size, Màu sắc &amp; Tồn kho)</label>
              <button type="button" class="btn-add-variant" @click="addVariantRow">+ Thêm Biến Thể</button>
            </div>

            <div v-for="(v, index) in formVariants" :key="index" class="variant-row">
              <input v-model="v.size" placeholder="Size (S/M/L/XL)" style="width: 100px;" required />
              <input v-model="v.color" placeholder="Màu (Đen/Trắng)" style="width: 120px;" required />
              <input v-model.number="v.stockQuantity" type="number" placeholder="Số lượng tồn" style="width: 110px;" required />
              <button type="button" class="btn-remove-v" @click="removeVariantRow(index)">❌</button>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="showModal = false">HỦY BỎ</button>
            <button type="submit" class="btn-save">
              {{ isEditing ? 'LƯU CẬP NHẬT' : 'TẠO SẢN PHẨM' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal Quản Lý Danh Mục (Admin Category Management CRUD) -->
    <div v-if="showCategoryModal" class="modal-overlay">
      <div class="modal-card">
        <div class="modal-header">
          <h3>📂 Quản Lý Danh Mục Thời Trang</h3>
          <button class="btn-close" @click="showCategoryModal = false">✖</button>
        </div>

        <!-- Category Add/Edit Form -->
        <form @submit.prevent="handleAddCategory" class="cat-form">
          <div class="form-grid">
            <div class="form-group">
              <label>Tên Danh Mục *</label>
              <input v-model="categoryNameInput" type="text" placeholder="Áo Thun, Quần Jean..." required />
            </div>
            <div class="form-group">
              <label>Mô tả ngắn</label>
              <input v-model="categoryDescInput" type="text" placeholder="Thời trang nam nữ..." />
            </div>
          </div>

          <button type="submit" class="btn-add-cat">
            {{ editingCatId ? '💾 CẬP NHẬT DANH MỤC' : '➕ THÊM DANH MỤC MỚI' }}
          </button>
          <button v-if="editingCatId" type="button" class="btn-cancel-edit-cat" @click="editingCatId = null; categoryNameInput = ''; categoryDescInput = ''">Hủy Sửa</button>
        </form>

        <!-- Existing Category List Table -->
        <div class="cat-table-wrapper">
          <h4>Danh Sách Danh Mục Hiện Có:</h4>
          <table class="cat-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>TÊN DANH MỤC</th>
                <th>SLUG</th>
                <th>THAO TÁC</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cat in categories" :key="cat.id">
                <td>#{{ cat.id }}</td>
                <td class="font-bold">{{ cat.name }}</td>
                <td><code>{{ cat.slug }}</code></td>
                <td>
                  <div class="action-buttons">
                    <button class="btn-edit" @click="startEditCategory(cat)">✏️ Sửa</button>
                    <button class="btn-delete" @click="handleDeleteCategory(cat)">🗑️ Xóa</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.badge-admin {
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

.btn-create {
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: white;
  border: none;
  padding: 0.85rem 1.5rem;
  border-radius: 12px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 6px 15px rgba(249, 115, 22, 0.25);
  transition: transform 0.2s ease;
}

.btn-category {
  background: white;
  border: 1.5px solid #0284c7;
  color: #0284c7;
  padding: 0.85rem 1.5rem;
  border-radius: 12px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-category:hover {
  background: #0284c7;
  color: white;
}

/* Stat Cards */
.inventory-summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.25rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 1.25rem 1.5rem;
  display: flex;
  align-items: center;
  gap: 1.25rem;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
  cursor: pointer;
}

.stat-card.warning {
  border-color: #fecaca;
  background: #fef2f2;
}

.stat-card.success {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.stat-icon { font-size: 2.2rem; }
.stat-value { font-size: 1.5rem; font-weight: 800; color: #0f172a; }
.text-red { color: #dc2626; }
.text-green { color: #16a34a; }
.stat-label { font-size: 0.75rem; font-weight: 700; color: #64748b; }

/* Low Stock Alert */
.low-stock-alert {
  background: #fff1f2;
  border: 1px solid #fecdd3;
  border-radius: 16px;
  padding: 1.25rem;
  margin-bottom: 2rem;
}

.low-stock-alert h4 { color: #e11d48; font-size: 0.9rem; margin-bottom: 0.75rem; }

.low-stock-chips { display: flex; gap: 0.75rem; flex-wrap: wrap; }

.chip-warning {
  background: white;
  border: 1px solid #fda4af;
  padding: 0.4rem 0.8rem;
  border-radius: 10px;
  font-size: 0.85rem;
  color: #881337;
}

.stock-badge {
  background: #ffe4e6;
  color: #e11d48;
  font-weight: 800;
  padding: 0.15rem 0.4rem;
  border-radius: 6px;
}

/* Quick Filter Pills Bar */
.table-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.table-title {
  font-size: 1.2rem;
  font-weight: 800;
  color: #0f172a;
}

.admin-search-box {
  display: flex;
  align-items: center;
  background: white;
  border: 1.5px solid #cbd5e1;
  border-radius: 12px;
  padding: 0.4rem 0.8rem;
  min-width: 240px;
}

.admin-search-box input {
  border: none;
  outline: none;
  font-size: 0.85rem;
  color: #0f172a;
  width: 100%;
  background: transparent;
}

.btn-clear-search {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
}

.stock-filter-pills {
  display: flex;
  gap: 0.5rem;
}

.pill-btn {
  background: white;
  border: 1px solid #cbd5e1;
  color: #475569;
  padding: 0.4rem 0.8rem;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
}

.pill-btn.active {
  background: #0284c7;
  color: white;
  border-color: #0284c7;
}

.pill-warning.active {
  background: #f59e0b;
  border-color: #f59e0b;
}

.pill-danger.active {
  background: #ef4444;
  border-color: #ef4444;
}

/* Table */
.table-container {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  overflow-x: auto;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
}

.product-table { width: 100%; border-collapse: collapse; text-align: left; }

.product-table th {
  background: #f8fafc;
  padding: 1rem 1.25rem;
  font-size: 0.75rem;
  font-weight: 800;
  color: #475569;
  border-bottom: 1px solid #e2e8f0;
}

.product-table td {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.9rem;
  color: #0f172a;
}

.thumb-img { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; }

.cat-tag {
  background: #e0f2fe;
  color: #0369a1;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.2rem 0.6rem;
  border-radius: 8px;
}

.stock-tag { font-weight: 800; font-size: 0.8rem; padding: 0.25rem 0.6rem; border-radius: 8px; }
.stock-ok { background: #dcfce7; color: #15803d; }
.stock-low { background: #fee2e2; color: #b91c1c; }

.action-buttons { display: flex; gap: 0.5rem; }

.btn-edit {
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #0284c7;
  padding: 0.4rem 0.75rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.8rem;
  cursor: pointer;
}

.btn-delete {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #ef4444;
  padding: 0.4rem 0.75rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.8rem;
  cursor: pointer;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  z-index: 2000;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1.5rem;
}

.modal-card {
  background: white;
  border-radius: 20px;
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 2rem;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.modal-header h3 { font-size: 1.4rem; font-weight: 800; color: #0f172a; }
.btn-close { background: none; border: none; font-size: 1.2rem; cursor: pointer; }

.modal-form { display: flex; flex-direction: column; gap: 1.25rem; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.form-group { display: flex; flex-direction: column; gap: 0.4rem; }
.form-group label { font-size: 0.85rem; font-weight: 700; color: #334155; }
.form-group input, .form-group select, .form-group textarea {
  background: #f8fafc;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  padding: 0.75rem 1rem;
  font-size: 0.9rem;
  color: #0f172a;
}

.variants-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1rem;
}

.variant-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
.btn-add-variant { background: #0284c7; color: white; border: none; padding: 0.3rem 0.75rem; border-radius: 6px; font-weight: 700; font-size: 0.75rem; cursor: pointer; }
.variant-row { display: flex; gap: 0.5rem; align-items: center; margin-bottom: 0.5rem; }
.btn-remove-v { background: none; border: none; cursor: pointer; }

.modal-footer { display: flex; justify-content: flex-end; gap: 1rem; margin-top: 1rem; }
.btn-cancel { background: #f1f5f9; color: #475569; border: 1px solid #cbd5e1; padding: 0.8rem 1.5rem; border-radius: 10px; font-weight: 700; cursor: pointer; }
.btn-save { background: linear-gradient(135deg, #f97316, #ea580c); color: white; border: none; padding: 0.8rem 1.75rem; border-radius: 10px; font-weight: 800; cursor: pointer; }

/* Category Modal Styles */
.cat-form { display: flex; flex-direction: column; gap: 1rem; margin-bottom: 2rem; background: #f8fafc; padding: 1.25rem; border-radius: 14px; border: 1px solid #e2e8f0; }
.btn-add-cat { background: #0284c7; color: white; border: none; padding: 0.75rem; border-radius: 8px; font-weight: 800; cursor: pointer; }
.btn-cancel-edit-cat { background: none; border: none; color: #64748b; font-weight: 700; cursor: pointer; }
.cat-table-wrapper h4 { font-size: 0.95rem; font-weight: 800; margin-bottom: 0.75rem; color: #0f172a; }
.cat-table { width: 100%; border-collapse: collapse; text-align: left; }
.cat-table th { background: #f1f5f9; padding: 0.6rem 0.8rem; font-size: 0.75rem; font-weight: 800; color: #475569; }
.cat-table td { padding: 0.6rem 0.8rem; border-bottom: 1px solid #f1f5f9; font-size: 0.85rem; }

.alert { padding: 0.75rem 1rem; border-radius: 10px; font-size: 0.85rem; margin-bottom: 1.5rem; }
.alert-error { background: #fef2f2; border: 1px solid #fecaca; color: #dc2626; }
.alert-success { background: #f0fdf4; border: 1px solid #bbf7d0; color: #16a34a; }

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.4rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
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

.file-upload-input-group {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.file-upload-input-group input[type="text"] {
  flex: 1;
}

.btn-upload-file {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
  padding: 0.6rem 1rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 800;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.btn-upload-file:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.img-preview-box {
  margin-top: 0.5rem;
}

.img-preview {
  width: 80px;
  height: 80px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #cbd5e1;
}

.clickable-prod-name {
  cursor: pointer;
  color: #0f172a;
  font-weight: 600;
  transition: all 0.15s ease;
}

.clickable-prod-name:hover {
  color: #0284c7;
  font-weight: 800;
  text-decoration: underline;
}

.clickable-thumb {
  cursor: pointer;
  transition: transform 0.15s ease;
}

.clickable-thumb:hover {
  transform: scale(1.08);
}
</style>

<!-- Feature Implementation: thiết kế ui form thêm & upload -->

<!-- Feature Implementation: ui load dữ liệu cũ vào form -->

<!-- Feature Implementation: ui bảng theo dõi tồn kho admin -->

<!-- Stage 2: Edit Modal Loaded Old Data from Server -->