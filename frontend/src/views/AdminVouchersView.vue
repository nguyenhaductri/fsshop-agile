<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { adminVoucherApi } from '../services/api';
import { authState } from '../state/authStore';

const emit = defineEmits(['navigate']);

const vouchers = ref([]);
const loading = ref(false);
const searchQuery = ref('');

// Modal state
const showModal = ref(false);
const isEditing = ref(false);
const editingId = ref(null);

// Form state
const formCustomId = ref('');
const formCode = ref('');
const formName = ref('');
const formDescription = ref('');
const formDiscountType = ref('PERCENT'); // 'PERCENT' or 'FIXED'
const formDiscountValue = ref(10);
const formMinOrderAmount = ref(0);
const formMaxDiscountAmount = ref(100000);
const formUsageLimit = ref(100);
const formStartDate = ref('');
const formEndDate = ref('');
const formIsPublic = ref(true);
const formStatus = ref(1);

// Pagination
const currentPage = ref(1);
const pageSize = 10;

onMounted(async () => {
  if (!authState.user || (authState.user.role !== 'ROLE_ADMIN' && authState.user.role !== 'ROLE_OWNER')) {
    emit('navigate', 'home');
    return;
  }
  await fetchVouchers();
});

async function fetchVouchers() {
  loading.value = true;
  try {
    const res = await adminVoucherApi.getAllVouchers();
    vouchers.value = res.data || [];
  } catch (err) {
    console.error('Lỗi tải danh sách Voucher:', err);
  } finally {
    loading.value = false;
  }
}

const filteredVouchers = computed(() => {
  let list = vouchers.value;
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase();
    list = list.filter(v =>
      (v.code && v.code.toLowerCase().includes(q)) ||
      (v.name && v.name.toLowerCase().includes(q))
    );
  }
  return list;
});

const totalPages = computed(() => {
  return Math.ceil(filteredVouchers.value.length / pageSize) || 1;
});

const paginatedVouchers = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredVouchers.value.slice(start, start + pageSize);
});

watch(searchQuery, () => {
  currentPage.value = 1;
});

function formatPrice(val) {
  if (!val && val !== 0) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}

function formatDate(dateStr) {
  if (!dateStr) return 'Không giới hạn';
  return new Date(dateStr).toLocaleDateString('vi-VN');
}

function openCreateModal() {
  isEditing.value = false;
  editingId.value = null;
  formCustomId.value = '';
  formCode.value = '';
  formName.value = '';
  formDescription.value = '';
  formDiscountType.value = 'PERCENT';
  formDiscountValue.value = 10;
  formMinOrderAmount.value = 0;
  formMaxDiscountAmount.value = 100000;
  formUsageLimit.value = 100;
  formStartDate.value = '';
  formEndDate.value = '';
  formIsPublic.value = true;
  formStatus.value = 1;
  showModal.value = true;
}

function openEditModal(v) {
  isEditing.value = true;
  editingId.value = v.id;
  formCustomId.value = v.id;
  formCode.value = v.code;
  formName.value = v.name;
  formDescription.value = v.description || '';
  formDiscountType.value = v.discountType || 'PERCENT';
  formDiscountValue.value = v.discountValue;
  formMinOrderAmount.value = v.minOrderAmount || 0;
  formMaxDiscountAmount.value = v.maxDiscountAmount || '';
  formUsageLimit.value = v.usageLimit || 100;
  formStartDate.value = v.startDate ? v.startDate.substring(0, 10) : '';
  formEndDate.value = v.endDate ? v.endDate.substring(0, 10) : '';
  formIsPublic.value = v.isPublic !== undefined ? v.isPublic : true;
  formStatus.value = v.status !== undefined ? v.status : 1;
  showModal.value = true;
}

async function handleSave() {
  if (!formCode.value || !formCode.value.trim()) {
    alert('Vui lòng nhập Mã Voucher (Code)!');
    return;
  }
  if (!formName.value || !formName.value.trim()) {
    alert('Vui lòng nhập Tên chương trình Voucher!');
    return;
  }
  if (formDiscountValue.value === null || formDiscountValue.value === undefined || Number(formDiscountValue.value) <= 0) {
    alert('Vui lòng nhập Giá trị giảm hợp lệ (> 0)!');
    return;
  }

  const payload = {
    id: formCustomId.value ? Number(formCustomId.value) : null,
    code: formCode.value.trim(),
    name: formName.value.trim(),
    description: formDescription.value.trim(),
    discountType: formDiscountType.value,
    discountValue: Number(formDiscountValue.value),
    minOrderAmount: Number(formMinOrderAmount.value || 0),
    maxDiscountAmount: formMaxDiscountAmount.value ? Number(formMaxDiscountAmount.value) : null,
    usageLimit: Number(formUsageLimit.value || 100),
    startDate: formStartDate.value ? `${formStartDate.value}T00:00:00` : null,
    endDate: formEndDate.value ? `${formEndDate.value}T23:59:59` : null,
    isPublic: Boolean(formIsPublic.value),
    status: Number(formStatus.value)
  };

  try {
    if (isEditing.value) {
      await adminVoucherApi.updateVoucher(editingId.value, payload);
      alert('Cập nhật Voucher thành công!');
    } else {
      await adminVoucherApi.createVoucher(payload);
      alert('Tạo mới Voucher thành công!');
    }
    showModal.value = false;
    await fetchVouchers();
  } catch (err) {
    alert(err.message || 'Lưu Voucher thất bại!');
  }
}

async function handleDelete(v) {
  if (confirm(`Bạn có chắc chắn muốn xóa Voucher '${v.code}'?`)) {
    try {
      await adminVoucherApi.deleteVoucher(v.id);
      await fetchVouchers();
      alert('Đã xóa Voucher thành công!');
    } catch (err) {
      alert(err.message || 'Xóa thất bại!');
    }
  }
}

async function toggleStatus(v) {
  try {
    const newStatus = v.status === 1 ? 0 : 1;
    await adminVoucherApi.updateVoucher(v.id, {
      ...v,
      status: newStatus
    });
    await fetchVouchers();
  } catch (err) {
    alert(err.message || 'Thao tác thất bại!');
  }
}
</script>

<template>
  <div class="admin-vouchers-page">
    <div class="page-header">
      <div>
        <div class="page-badge">ADMIN • QUẢN LÝ KHUYẾN MÃI</div>
        <h1 class="page-title">🎟️ Danh Sách Voucher & Mã Giảm Giá</h1>
        <p class="page-subtitle">Tạo mã giảm giá theo % hoặc tiền cố định cho khách hàng</p>
      </div>
      <button class="btn-create" @click="openCreateModal">➕ Tạo Voucher Mới</button>
    </div>

    <!-- Filter & Search Bar -->
    <div class="filter-bar">
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input v-model="searchQuery" type="text" placeholder="Tìm theo Mã hoặc Tên voucher..." />
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Đang tải danh sách Voucher...</p>
    </div>

    <!-- Vouchers Table -->
    <div class="table-container" v-else>
      <table class="voucher-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>MÃ VOUCHER</th>
            <th>TÊN CHƯƠNG TRÌNH</th>
            <th>LOẠI GIẢM</th>
            <th>GIÁ TRỊ GIẢM</th>
            <th>ĐƠN TỐI THIỂU</th>
            <th>HIỂN THỊ</th>
            <th>LƯỢT DÙNG</th>
            <th>HẠN SỬ DỤNG</th>
            <th>TRẠNG THÁI</th>
            <th>THAO TÁC</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="v in paginatedVouchers" :key="v.id">
            <td class="id-cell">#{{ v.id }}</td>
            <td>
              <code class="code-badge">{{ v.code }}</code>
            </td>
            <td>
              <strong class="voucher-name">{{ v.name }}</strong>
              <div class="voucher-desc" v-if="v.description">{{ v.description }}</div>
            </td>
            <td>
              <span :class="['type-tag', v.discountType === 'PERCENT' ? 'type-percent' : 'type-fixed']">
                {{ v.discountType === 'PERCENT' ? 'Phần trăm %' : 'Cố định VNĐ' }}
              </span>
            </td>
            <td>
              <strong class="discount-val">
                {{ v.discountType === 'PERCENT' ? `-${v.discountValue}%` : `-${formatPrice(v.discountValue)}` }}
              </strong>
              <div v-if="v.discountType === 'PERCENT' && v.maxDiscountAmount" class="max-hint">
                Tối đa {{ formatPrice(v.maxDiscountAmount) }}
              </div>
            </td>
            <td>{{ formatPrice(v.minOrderAmount) }}</td>
            <td>
              <span :class="['public-tag', v.isPublic !== false ? 'pub-public' : 'pub-private']">
                {{ v.isPublic !== false ? '🌐 Công khai' : '🔒 Bí mật / Ẩn' }}
              </span>
            </td>
            <td>
              <span class="usage-count">{{ v.usedCount }}/{{ v.usageLimit || '∞' }}</span>
            </td>
            <td>
              <div class="date-range">
                <span>{{ formatDate(v.startDate) }}</span>
                <span>➜ {{ formatDate(v.endDate) }}</span>
              </div>
            </td>
            <td>
              <button 
                :class="['status-toggle', v.status === 1 ? 'status-active' : 'status-disabled']"
                @click="toggleStatus(v)"
              >
                {{ v.status === 1 ? '🟢 Hoạt động' : '🔴 Tắt' }}
              </button>
            </td>
            <td>
              <div class="action-btns">
                <button class="btn-edit" @click="openEditModal(v)">✏️ Sửa</button>
                <button class="btn-delete" @click="handleDelete(v)">🗑️ Xóa</button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredVouchers.length === 0">
            <td colspan="10" class="empty-cell">Không tìm thấy mã giảm giá nào phù hợp.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination Controls -->
    <div class="pagination-wrapper">
      <div class="pagination-info">
        Hiển thị {{ paginatedVouchers.length }} / {{ filteredVouchers.length }} Voucher (Trang {{ currentPage }} / {{ totalPages }})
      </div>
      <div class="pagination-btns" v-if="totalPages > 1">
        <button class="page-btn" :disabled="currentPage === 1" @click="currentPage--">&laquo; Trước</button>
        <button 
          v-for="page in totalPages" 
          :key="page" 
          :class="['page-btn', currentPage === page ? 'active' : '']"
          @click="currentPage = page"
        >
          {{ page }}
        </button>
        <button class="page-btn" :disabled="currentPage === totalPages" @click="currentPage++">Sau &raquo;</button>
      </div>
    </div>

    <!-- Modal Form Thêm/Sửa Voucher -->
    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>{{ isEditing ? '✏️ Chỉnh Sửa Voucher' : '➕ Tạo Voucher Khuyến Mãi Mới' }}</h3>
          <button class="btn-close" @click="showModal = false">✖</button>
        </div>

        <form @submit.prevent="handleSave" class="modal-body" novalidate>
          <div class="form-grid">
            <div class="form-group full-width">
              <label>Mã ID (Tùy chọn - Để trống sẽ tự động tăng)</label>
              <input 
                v-model="formCustomId" 
                type="number" 
                min="1" 
                placeholder="Nhập ID thủ công (Ví dụ: 10, 100...) hoặc ĐỂ TRỐNG tự động tăng" 
                :disabled="isEditing" 
              />
            </div>

            <div class="form-group">
              <label>Mã Voucher (Code) *</label>
              <input v-model="formCode" type="text" placeholder="Ví dụ: SUMMER2026, GIAM50K" />
            </div>

            <div class="form-group">
              <label>Tên chương trình *</label>
              <input v-model="formName" type="text" placeholder="Ví dụ: Giảm 50k cho đơn từ 200k" />
            </div>

            <div class="form-group full-width">
              <label>Mô tả chi tiết (Tùy chọn)</label>
              <input v-model="formDescription" type="text" placeholder="Ghi chú thêm cho người dùng..." />
            </div>

            <div class="form-group">
              <label>Loại giảm giá *</label>
              <select v-model="formDiscountType">
                <option value="PERCENT">Giảm theo Phần Trăm (%)</option>
                <option value="FIXED">Giảm Cố Định (VNĐ)</option>
              </select>
            </div>

            <div class="form-group">
              <label>Giá trị giảm ({{ formDiscountType === 'PERCENT' ? '%' : 'VNĐ' }}) *</label>
              <input v-model="formDiscountValue" type="number" min="1" placeholder="10 hoặc 50000" />
            </div>

            <div class="form-group">
              <label>Đơn hàng tối thiểu (VNĐ)</label>
              <input v-model="formMinOrderAmount" type="number" min="0" placeholder="Ví dụ: 200000" />
            </div>

            <div class="form-group" v-if="formDiscountType === 'PERCENT'">
              <label>Giảm tối đa (VNĐ)</label>
              <input v-model="formMaxDiscountAmount" type="number" min="0" placeholder="Ví dụ: 100000 (để trống nếu không giới hạn)" />
            </div>

            <div class="form-group">
              <label>Tổng số lượt sử dụng tối đa</label>
              <input v-model="formUsageLimit" type="number" min="1" placeholder="100" />
            </div>

            <div class="form-group">
              <label>Chế độ hiển thị *</label>
              <select v-model="formIsPublic">
                <option :value="true">🌐 Công khai (Khách thấy & chọn được)</option>
                <option :value="false">🔒 Bí mật / Ẩn (Chỉ dùng khi tự nhập mã)</option>
              </select>
            </div>

            <div class="form-group">
              <label>Trạng thái</label>
              <select v-model="formStatus">
                <option :value="1">🟢 Hoạt động</option>
                <option :value="0">🔴 Tắt / Khóa</option>
              </select>
            </div>

            <div class="form-group">
              <label>Ngày bắt đầu</label>
              <input v-model="formStartDate" type="date" />
            </div>

            <div class="form-group">
              <label>Ngày kết thúc</label>
              <input v-model="formEndDate" type="date" />
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="showModal = false">Hủy Bỏ</button>
            <button type="submit" class="btn-save">{{ isEditing ? 'Cập Nhật Voucher' : 'Tạo Mới Voucher' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-vouchers-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem 5rem;
}

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

.page-subtitle { color: #64748b; font-size: 0.9rem; }

.btn-create {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: white;
  border: none;
  padding: 0.8rem 1.5rem;
  border-radius: 12px;
  font-weight: 800;
  font-size: 0.9rem;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
  transition: transform 0.15s;
}

.btn-create:hover { transform: translateY(-2px); }

.filter-bar { margin-bottom: 1.5rem; }

.search-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: white;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 0.6rem 1rem;
  max-width: 400px;
}

.search-box input {
  border: none;
  outline: none;
  width: 100%;
  font-size: 0.9rem;
}

.table-container {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  overflow-x: auto;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
}

.voucher-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
  min-width: 950px;
}

.voucher-table th {
  background: #f8fafc;
  padding: 0.85rem 1rem;
  text-align: left;
  font-size: 0.78rem;
  font-weight: 800;
  color: #64748b;
  border-bottom: 1px solid #e2e8f0;
}

.voucher-table td {
  padding: 1rem;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.id-cell {
  font-weight: 800;
  color: #64748b;
  font-size: 0.85rem;
}

.code-badge {
  background: #eef2ff;
  color: #4f46e5;
  padding: 0.35rem 0.65rem;
  border-radius: 8px;
  font-weight: 800;
  font-family: monospace;
  font-size: 0.95rem;
  border: 1px dashed #c7d2fe;
}

.voucher-name { color: #0f172a; font-weight: 700; }
.voucher-desc { font-size: 0.8rem; color: #64748b; margin-top: 0.15rem; }

.type-tag {
  display: inline-block;
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 800;
}

.type-percent { background: #fef3c7; color: #d97706; }
.type-fixed { background: #e0f2fe; color: #0369a1; }

.public-tag {
  display: inline-block;
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 800;
}
.pub-public { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }
.pub-private { background: #f8fafc; color: #64748b; border: 1px solid #cbd5e1; }

.discount-val { color: #16a34a; font-size: 0.95rem; }
.max-hint { font-size: 0.72rem; color: #64748b; }

.usage-count { font-weight: 700; color: #334155; }

.date-range { font-size: 0.78rem; color: #64748b; display: flex; flex-direction: column; gap: 0.1rem; }

.status-toggle {
  border: none;
  background: none;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  padding: 0.3rem 0.6rem;
  border-radius: 6px;
}

.status-active { background: #f0fdf4; color: #16a34a; }
.status-disabled { background: #fef2f2; color: #dc2626; }

.action-btns { display: flex; gap: 0.5rem; }
.btn-edit { background: #f1f5f9; color: #334155; border: 1px solid #cbd5e1; padding: 0.35rem 0.65rem; border-radius: 6px; font-size: 0.8rem; font-weight: 700; cursor: pointer; }
.btn-delete { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; padding: 0.35rem 0.65rem; border-radius: 6px; font-size: 0.8rem; font-weight: 700; cursor: pointer; }

.empty-cell { text-align: center; padding: 2.5rem; color: #64748b; }

/* Modal */
.modal-backdrop {
  position: fixed;
  top: 0; left: 0; width: 100vw; height: 100vh;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex; justify-content: center; align-items: center;
  z-index: 1000; padding: 1rem;
}

.modal-card {
  background: white;
  border-radius: 20px;
  width: 100%;
  max-width: 650px;
  padding: 2rem;
  box-shadow: 0 10px 40px rgba(0,0,0,0.15);
}

.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.modal-header h3 { font-size: 1.25rem; font-weight: 800; color: #0f172a; margin: 0; }
.btn-close { background: none; border: none; font-size: 1.2rem; cursor: pointer; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.full-width { grid-column: span 2; }
.form-group { display: flex; flex-direction: column; gap: 0.35rem; }
.form-group label { font-size: 0.82rem; font-weight: 700; color: #334155; }
.form-group input, .form-group select {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0.6rem 0.85rem;
  font-size: 0.9rem;
  outline: none;
}

.modal-footer { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem; }
.btn-cancel { background: #f1f5f9; color: #475569; border: 1px solid #cbd5e1; padding: 0.6rem 1.2rem; border-radius: 8px; font-weight: 700; cursor: pointer; }
.btn-save { background: #6366f1; color: white; border: none; padding: 0.6rem 1.4rem; border-radius: 8px; font-weight: 800; cursor: pointer; }

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
  flex-wrap: wrap;
}

.pagination-info {
  font-size: 0.85rem;
  font-weight: 700;
  color: #64748b;
}

.pagination-btns {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.page-btn {
  background: white; border: 1px solid #cbd5e1; color: #334155; padding: 0.4rem 0.8rem; border-radius: 8px; font-size: 0.85rem; font-weight: 700; cursor: pointer;
}
.page-btn.active { background: #6366f1; border-color: #6366f1; color: white; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>

<!-- Feature Implementation: làm hệ thống mã giảm giá voucher và quản lý admin voucher -->
