<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { adminUserApi } from '../services/api';
import { authState } from '../state/authStore';

const emit = defineEmits(['navigate']);

const users = ref([]);
const loading = ref(false);
const searchQuery = ref('');
const roleFilter = ref('ALL');

// Modal State
const showCreateModal = ref(false);
const createForm = ref({
  username: '',
  password: '',
  email: '',
  fullName: '',
  phone: '',
  role: 'ROLE_ADMIN',
});
const createLoading = ref(false);

onMounted(async () => {
  if (!authState.user || authState.user.role !== 'ROLE_OWNER') {
    alert('Trang này chỉ dành cho Chủ Sở Hữu (Owner)!');
    emit('navigate', 'home');
    return;
  }
  await fetchUsers();
});

watch([searchQuery, roleFilter], () => {
  fetchUsers();
});

async function fetchUsers() {
  loading.value = true;
  try {
    const res = await adminUserApi.getUsers({
      search: searchQuery.value,
      role: roleFilter.value,
    });
    users.value = res.data || [];
  } catch (err) {
    console.error('Lỗi tải danh sách người dùng:', err);
    alert(err.message || 'Lỗi nạp danh sách tài khoản từ máy chủ!');
  } finally {
    loading.value = false;
  }
}

// Summary Stats
const totalUsersCount = computed(() => users.value.length);
const customerCount = computed(() => users.value.filter(u => u.role === 'ROLE_USER').length);
const adminCount = computed(() => users.value.filter(u => u.role === 'ROLE_ADMIN').length);
const ownerCount = computed(() => users.value.filter(u => u.role === 'ROLE_OWNER').length);
const lockedCount = computed(() => users.value.filter(u => u.status === 0).length);

async function handleRoleChange(user, newRole) {
  if (user.role === 'ROLE_OWNER' || user.username === 'owner') {
    alert('Tài khoản Chủ Sở Hữu (Owner) là cố định, không thể thay đổi vai trò!');
    await fetchUsers();
    return;
  }

  try {
    await adminUserApi.updateRole(user.id, newRole);
    alert(`Đã đổi vai trò tài khoản [${user.username}] thành ${getRoleBadgeText(newRole)}!`);
    await fetchUsers();
  } catch (err) {
    alert(err.message || 'Đổi vai trò thất bại!');
    await fetchUsers();
  }
}

async function handleToggleStatus(user) {
  if (user.role === 'ROLE_OWNER' || user.username === 'owner') {
    alert('Tài khoản Chủ Sở Hữu (Owner) là tài khoản tối cao, không thể bị khóa!');
    return;
  }

  const nextStatus = user.status === 1 ? 0 : 1;
  const actionText = nextStatus === 0 ? 'KHÓA' : 'MỞ KHÓA';
  if (confirm(`Bạn có chắc chắn muốn ${actionText} tài khoản [${user.username}]?`)) {
    try {
      await adminUserApi.updateStatus(user.id, nextStatus);
      await fetchUsers();
    } catch (err) {
      alert(err.message || 'Thao tác thất bại!');
    }
  }
}

function openCreateModal() {
  createForm.value = {
    username: '',
    password: '',
    email: '',
    fullName: '',
    phone: '',
    role: 'ROLE_ADMIN',
  };
  showCreateModal.value = true;
}

async function handleCreateUser() {
  if (!createForm.value.username.trim()) {
    alert('Vui lòng nhập Tên đăng nhập!');
    return;
  }

  createLoading.value = true;
  try {
    await adminUserApi.createUser(createForm.value);
    showCreateModal.value = false;
    await fetchUsers();
    alert('Tạo tài khoản mới thành công!');
  } catch (err) {
    alert(err.message || 'Tạo tài khoản thất bại!');
  } finally {
    createLoading.value = false;
  }
}

function getRoleBadgeText(role) {
  switch (role) {
    case 'ROLE_OWNER': return 'Chủ sở hữu (Owner)';
    case 'ROLE_ADMIN': return 'Quản trị viên (Admin)';
    case 'ROLE_USER': return 'Khách hàng';
    default: return role;
  }
}

function getRoleBadgeClass(role) {
  switch (role) {
    case 'ROLE_OWNER': return 'role-owner';
    case 'ROLE_ADMIN': return 'role-admin';
    case 'ROLE_USER': return 'role-user';
    default: return '';
  }
}
</script>

<template>
  <div class="admin-users-page">
    <div class="header-section">
      <div>
        <h2>📋 Quản Lý Tài Khoản & Phân Quyền</h2>
        <p class="subtitle">Trang quản trị dành riêng cho Chủ Sở Hữu hệ thống</p>
      </div>
      <button class="btn-create-user" @click="openCreateModal">
        ➕ Tạo Tài Khoản Mới
      </button>
    </div>

    <!-- Summary Stats Bar -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon">👥</div>
        <div class="stat-info">
          <span class="stat-num">{{ totalUsersCount }}</span>
          <span class="stat-label">Tổng Số Tài Khoản</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">👔</div>
        <div class="stat-info">
          <span class="stat-num color-owner">{{ ownerCount }}</span>
          <span class="stat-label">Chủ Sở Hữu (Owner)</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">⚙️</div>
        <div class="stat-info">
          <span class="stat-num color-admin">{{ adminCount }}</span>
          <span class="stat-label">Quản Trị Viên (Admin)</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">👤</div>
        <div class="stat-info">
          <span class="stat-num color-user">{{ customerCount }}</span>
          <span class="stat-label">Khách Hàng</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon">🔒</div>
        <div class="stat-info">
          <span class="stat-num color-locked">{{ lockedCount }}</span>
          <span class="stat-label">Tài Khoản Đang Khóa</span>
        </div>
      </div>
    </div>

    <!-- Toolbar: Search & Role Filter -->
    <div class="toolbar-box">
      <div class="search-box">
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="🔍 Tìm kiếm theo tên đăng nhập, họ tên, email, SĐT..." 
        />
      </div>

      <div class="filter-box">
        <label>Lọc vai trò:</label>
        <select v-model="roleFilter" class="select-role-filter">
          <option value="ALL">Tất cả vai trò</option>
          <option value="ROLE_OWNER">Chủ Sở Hữu (Owner)</option>
          <option value="ROLE_ADMIN">Quản Trị Viên (Admin)</option>
          <option value="ROLE_USER">Khách Hàng</option>
        </select>
      </div>
    </div>

    <!-- Users Table -->
    <div class="table-container">
      <div v-if="loading" class="loading-box">
        ⏳ Đang nạp danh sách tài khoản...
      </div>

      <table v-else class="users-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Tên Đăng Nhập</th>
            <th>Họ & Tên</th>
            <th>Email</th>
            <th>Số Điện Thoại</th>
            <th>Vai Trò (Role)</th>
            <th>Trạng Thái</th>
            <th>Thao Tác Quản Trị</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id" :class="{'current-user-row': u.id === authState.user.id}">
            <td>#{{ u.id }}</td>
            <td class="font-bold">
              {{ u.username }}
              <span v-if="u.id === authState.user.id" class="badge-you">(Bạn)</span>
            </td>
            <td>{{ u.fullName || '---' }}</td>
            <td>{{ u.email || '---' }}</td>
            <td>{{ u.phone || '---' }}</td>
            <td>
              <span v-if="u.role === 'ROLE_OWNER' || u.username === 'owner'" class="status-badge role-owner-badge">
                👔 Chủ Sở Hữu (Cố định)
              </span>
              <select 
                v-else
                :value="u.role" 
                @change="e => handleRoleChange(u, e.target.value)"
                :class="['select-role-table', getRoleBadgeClass(u.role)]"
              >
                <option value="ROLE_ADMIN">Quản Trị Viên</option>
                <option value="ROLE_USER">Khách Hàng</option>
              </select>
            </td>
            <td>
              <span :class="['status-badge', u.status === 1 ? 'status-active' : 'status-locked']">
                {{ u.status === 1 ? '🟢 Hoạt động' : '🔴 Đã khóa' }}
              </span>
            </td>
            <td>
              <button 
                :class="['btn-status-toggle', u.status === 1 ? 'btn-lock' : 'btn-unlock']"
                @click="handleToggleStatus(u)"
                :disabled="u.role === 'ROLE_OWNER' || u.username === 'owner'"
              >
                {{ u.status === 1 ? '🔒 Khóa' : '🔓 Mở Khóa' }}
              </button>
            </td>
          </tr>

          <tr v-if="!users.length">
            <td colspan="8" class="empty-table-msg">
              Không tìm thấy tài khoản nào phù hợp với bộ lọc.
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create User Modal -->
    <div v-if="showCreateModal" class="modal-backdrop" @click.self="showCreateModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>➕ Tạo Tài Khoản Mới</h3>
          <button class="btn-close" @click="showCreateModal = false">✖</button>
        </div>

        <form @submit.prevent="handleCreateUser" class="modal-form">
          <div class="form-group">
            <label>Tên Đăng Nhập *</label>
            <input v-model="createForm.username" type="text" placeholder="Nhập tên đăng nhập (ví dụ: admin2)..." required />
          </div>

          <div class="form-group">
            <label>Mật Khẩu (Mặc định: 123456)</label>
            <input v-model="createForm.password" type="text" placeholder="123456" />
          </div>

          <div class="form-group">
            <label>Vai Trò (Phân Quyền) *</label>
            <select v-model="createForm.role" class="select-role-input">
              <option value="ROLE_ADMIN">Quản trị viên (Admin)</option>
              <option value="ROLE_USER">Khách Hàng (Customer)</option>
            </select>
          </div>

          <div class="form-group">
            <label>Họ và Tên</label>
            <input v-model="createForm.fullName" type="text" placeholder="Nhập họ và tên..." />
          </div>

          <div class="form-group">
            <label>Email</label>
            <input v-model="createForm.email" type="email" placeholder="email@fsshop.com..." />
          </div>

          <div class="form-group">
            <label>Số Điện Thoại</label>
            <input v-model="createForm.phone" type="text" placeholder="0988..." />
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="showCreateModal = false">Hủy</button>
            <button type="submit" class="btn-submit" :disabled="createLoading">
              {{ createLoading ? '⏳ Đang tạo...' : 'TẠO TÀI KHOẢN' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-users-page {
  max-width: 1250px;
  margin: 0 auto;
  padding: 2rem 1.5rem 4rem 1.5rem;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.header-section h2 {
  font-size: 1.8rem;
  font-weight: 900;
  color: #0f172a;
}

.subtitle {
  color: #64748b;
  font-size: 0.9rem;
}

.btn-create-user {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  color: white;
  border: none;
  padding: 0.7rem 1.4rem;
  border-radius: 12px;
  font-weight: 800;
  font-size: 0.9rem;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.3);
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-create-user:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(2, 132, 199, 0.4);
}

/* Stats Cards */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.25rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 1.25rem;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon {
  font-size: 2rem;
}

.stat-num {
  font-size: 1.5rem;
  font-weight: 900;
  color: #0f172a;
  display: block;
}

.color-owner { color: #d97706; }
.color-admin { color: #0284c7; }
.color-user { color: #475569; }
.color-locked { color: #dc2626; }

.stat-label {
  font-size: 0.75rem;
  color: #64748b;
  font-weight: 700;
}

/* Toolbar */
.toolbar-box {
  display: flex;
  gap: 1rem;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 1rem;
  margin-bottom: 1.5rem;
}

.search-box {
  flex: 1;
}

.search-box input {
  width: 100%;
  border: 1px solid #cbd5e1;
  padding: 0.6rem 1rem;
  border-radius: 10px;
  font-size: 0.88rem;
  outline: none;
}

.filter-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  font-weight: 700;
  color: #475569;
}

.select-role-filter {
  border: 1px solid #cbd5e1;
  padding: 0.6rem 1rem;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 700;
}

/* Table */
.table-container {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  overflow: hidden;
}

.users-table {
  width: 100%;
  border-collapse: collapse;
}

.users-table th {
  background: #f8fafc;
  color: #475569;
  font-size: 0.8rem;
  font-weight: 800;
  text-transform: uppercase;
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.users-table td {
  padding: 1rem;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.88rem;
  color: #334155;
}

.current-user-row {
  background: #faf5ff;
}

.badge-you {
  background: #f3e8ff;
  color: #9333ea;
  font-size: 0.7rem;
  font-weight: 800;
  padding: 0.15rem 0.4rem;
  border-radius: 6px;
  margin-left: 0.3rem;
}

.select-role-table {
  border: 1px solid #cbd5e1;
  padding: 0.35rem 0.65rem;
  border-radius: 8px;
  font-size: 0.8rem;
  font-weight: 800;
}

.role-owner { background: #fef3c7; color: #d97706; border-color: #fcd34d; }
.role-owner-badge { background: #fef3c7; color: #b45309; border: 1px solid #fcd34d; display: inline-block; }
.role-admin { background: #e0f2fe; color: #0284c7; border-color: #7dd3fc; }
.role-user { background: #f1f5f9; color: #475569; }

.status-badge {
  font-size: 0.8rem;
  font-weight: 800;
  padding: 0.25rem 0.65rem;
  border-radius: 20px;
}

.status-active { background: #dcfce7; color: #15803d; }
.status-locked { background: #fef2f2; color: #dc2626; }

.btn-status-toggle {
  border: none;
  padding: 0.35rem 0.75rem;
  border-radius: 8px;
  font-weight: 800;
  font-size: 0.78rem;
  cursor: pointer;
}

.btn-lock { background: #fef2f2; color: #dc2626; border: 1px solid #fca5a5; }
.btn-unlock { background: #f0fdf4; color: #16a34a; border: 1px solid #86efac; }
.btn-status-toggle:disabled { opacity: 0.4; cursor: not-allowed; }

.loading-box, .empty-table-msg {
  text-align: center;
  padding: 3rem;
  color: #64748b;
}

/* Modal */
.modal-backdrop {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-card {
  background: white;
  border-radius: 20px;
  padding: 2rem;
  width: 100%;
  max-width: 480px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.modal-header h3 {
  font-size: 1.2rem;
  font-weight: 900;
  color: #0f172a;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.1rem;
  color: #94a3b8;
  cursor: pointer;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.modal-form .form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.modal-form label {
  font-size: 0.8rem;
  font-weight: 800;
  color: #475569;
}

.modal-form input, .select-role-input {
  border: 1px solid #cbd5e1;
  padding: 0.6rem 0.85rem;
  border-radius: 10px;
  font-size: 0.88rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
}

.btn-cancel {
  background: #f1f5f9;
  border: none;
  padding: 0.6rem 1.2rem;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
}

.btn-submit {
  background: #6366f1;
  color: white;
  border: none;
  padding: 0.6rem 1.5rem;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
}
</style>

<!-- Feature Implementation: thêm tính năng quản lý tài khoản người dùng cho admin -->
