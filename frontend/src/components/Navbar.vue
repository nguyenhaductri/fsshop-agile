<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { authState } from '../state/authStore';
import { cartState } from '../state/cartStore';
import { notificationApi } from '../services/api';

const emit = defineEmits(['navigate']);

const notifications = ref([]);
const unreadCount = ref(0);
const showNotifDropdown = ref(false);
const notifWrapperRef = ref(null);
let pollInterval = null;

function handleOutsideClick(event) {
  if (showNotifDropdown.value && notifWrapperRef.value && !notifWrapperRef.value.contains(event.target)) {
    showNotifDropdown.value = false;
  }
}

onMounted(async () => {
  document.addEventListener('click', handleOutsideClick);
  if (authState.user) {
    await cartState.fetchCart();
    await fetchNotifications();
    pollInterval = setInterval(fetchNotifications, 10000); // Poll notifications every 10 seconds
  }
});

onUnmounted(() => {
  document.removeEventListener('click', handleOutsideClick);
  if (pollInterval) clearInterval(pollInterval);
});

async function fetchNotifications() {
  if (!authState.user) return;
  try {
    const [listRes, countRes] = await Promise.all([
      notificationApi.getNotifications(authState.user.id),
      notificationApi.getUnreadCount(authState.user.id)
    ]);
    notifications.value = listRes.data || [];
    unreadCount.value = countRes.data || 0;
  } catch (err) {
    console.error('Lỗi tải thông báo:', err);
  }
}

function toggleNotifDropdown() {
  showNotifDropdown.value = !showNotifDropdown.value;
  if (showNotifDropdown.value) {
    fetchNotifications();
  }
}

async function handleNotifClick(notif) {
  if (!notif.isRead) {
    try {
      await notificationApi.markAsRead(authState.user.id, notif.id);
      notif.isRead = true;
      if (unreadCount.value > 0) unreadCount.value--;
    } catch (err) {
      console.error(err);
    }
  }
  showNotifDropdown.value = false;
  if (notif.link) {
    if (notif.link.startsWith('product-detail:')) {
      const prodId = Number(notif.link.split(':')[1]);
      emit('navigate', 'product-detail', prodId);
    } else {
      emit('navigate', notif.link);
    }
  }
}

async function handleMarkAllRead() {
  if (!authState.user) return;
  try {
    await notificationApi.markAllAsRead(authState.user.id);
    notifications.value.forEach(n => n.isRead = true);
    unreadCount.value = 0;
  } catch (err) {
    console.error(err);
  }
}

function handleLogout() {
  if (pollInterval) clearInterval(pollInterval);
  authState.logout();
  cartState.items = [];
  cartState.totalItems = 0;
  notifications.value = [];
  unreadCount.value = 0;
  showNotifDropdown.value = false;
  emit('navigate', 'login');
}

function formatNotifTime(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' });
}
</script>

<template>
  <header class="navbar">
    <div class="nav-container">
      <div class="brand" @click="emit('navigate', 'home')">
        <div class="brand-logo">FS</div>
        <div class="brand-text">
          <span class="brand-name">FS SHOP</span>
          <span class="brand-tag">FASHION STORE</span>
        </div>
      </div>

      <nav class="nav-links">
        <button class="nav-btn" @click="emit('navigate', 'home')">Trang Chủ</button>
        <button class="nav-btn" @click="emit('navigate', 'products')">Sản Phẩm</button>
        <button class="nav-btn btn-cart-nav" @click="emit('navigate', 'cart')">
          🛒 Giỏ Hàng
          <span v-if="cartState.totalItems > 0" class="cart-badge">{{ cartState.totalItems }}</span>
        </button>
        <button v-if="authState.user" class="nav-btn" @click="emit('navigate', 'orders')">
          📋 Đơn Hàng
        </button>

        <!-- Notification Bell Icon & Dropdown -->
        <div v-if="authState.user" class="notif-wrapper" ref="notifWrapperRef">
          <button class="nav-btn btn-notif-nav" @click="toggleNotifDropdown">
            🔔 Thông Báo
            <span v-if="unreadCount > 0" class="notif-badge">{{ unreadCount }}</span>
          </button>

          <!-- Notification Dropdown Panel -->
          <div v-if="showNotifDropdown" class="notif-dropdown">
            <div class="notif-header">
              <strong>🔔 Thông Báo</strong>
              <button v-if="unreadCount > 0" class="btn-mark-all" @click="handleMarkAllRead">✓ Đã đọc tất cả</button>
            </div>

            <div class="notif-list">
              <div
                v-for="notif in notifications"
                :key="notif.id"
                :class="['notif-item', notif.isRead ? 'read' : 'unread']"
                @click="handleNotifClick(notif)"
              >
                <div class="notif-item-header">
                  <span class="notif-title">{{ notif.title }}</span>
                  <span class="notif-time">{{ formatNotifTime(notif.createdAt) }}</span>
                </div>
                <div class="notif-msg">{{ notif.message }}</div>
              </div>

              <div v-if="notifications.length === 0" class="empty-notif">
                Bạn chưa có thông báo nào.
              </div>
            </div>
          </div>
        </div>
      </nav>

      <div class="auth-section">
        <template v-if="authState.user">
          <div class="user-dropdown-wrapper">
            <div class="user-badge" @click="emit('navigate', 'profile')">
              <img
                :src="authState.user.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'"
                alt="Avatar"
                class="avatar"
              />
              <span class="username">{{ authState.user.fullName || authState.user.username }}</span>
              <span v-if="authState.user.role === 'ROLE_ADMIN'" class="role-tag">ADMIN</span>
              <span v-else-if="authState.user.role === 'ROLE_OWNER'" class="role-tag role-tag-owner">OWNER</span>
              <span class="dropdown-arrow">▾</span>
            </div>

            <!-- Hover User Dropdown Menu -->
            <div class="user-dropdown-menu">
              <div class="user-menu-header">
                <img
                  :src="authState.user.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'"
                  alt="Avatar"
                  class="menu-avatar"
                />
                <div class="menu-user-info">
                  <div class="menu-name">{{ authState.user.fullName || authState.user.username }}</div>
                  <div class="menu-email">{{ authState.user.email || '@' + authState.user.username }}</div>
                </div>
              </div>

              <div class="menu-divider"></div>

              <button class="user-menu-item" @click="emit('navigate', 'profile')">
                <span class="menu-icon">👤</span> Chỉnh sửa thông tin
              </button>

              <!-- Admin & Owner Management Links inside User Dropdown -->
              <template v-if="authState.user.role === 'ROLE_ADMIN' || authState.user.role === 'ROLE_OWNER'">
                <div class="menu-section-label">
                  {{ authState.user.role === 'ROLE_OWNER' ? 'CHỦ SỞ HỮU (OWNER)' : 'QUẢN TRỊ VIÊN' }}
                </div>

                <button class="user-menu-item admin-item" @click="emit('navigate', 'admin-orders')">
                  <span class="menu-icon">⚙️</span> Quản lý Đơn hàng
                </button>

                <button class="user-menu-item admin-item" @click="emit('navigate', 'admin-products')">
                  <span class="menu-icon">📦</span> Quản lý Kho sản phẩm
                </button>

                <button class="user-menu-item admin-item" @click="emit('navigate', 'admin-vouchers')">
                  <span class="menu-icon">🎟️</span> Quản lý Voucher
                </button>

                <button class="user-menu-item admin-item" @click="emit('navigate', 'admin-dashboard')">
                  <span class="menu-icon">📊</span> Thống kê doanh thu
                </button>

                <button v-if="authState.user.role === 'ROLE_OWNER'" class="user-menu-item owner-item" @click="emit('navigate', 'admin-users')">
                  <span class="menu-icon">📋</span> Quản lý Tài khoản & Phân quyền
                </button>
              </template>

              <div class="menu-divider"></div>

              <button class="user-menu-item logout-item" @click="handleLogout">
                <span class="menu-icon">🚪</span> Đăng xuất
              </button>
            </div>
          </div>
        </template>
        <template v-else>
          <button class="btn-login" @click="emit('navigate', 'login')">Đăng Nhập</button>
          <button class="btn-register" @click="emit('navigate', 'register')">Đăng Ký</button>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.navbar {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #e2e8f0;
  padding: 0.75rem 1.5rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.nav-container {
  max-width: 1300px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  cursor: pointer;
  flex-shrink: 0;
}

.brand-logo {
  background: linear-gradient(135deg, #f97316, #0284c7);
  color: white;
  font-weight: 900;
  font-size: 1.1rem;
  padding: 0.4rem 0.6rem;
  border-radius: 10px;
  letter-spacing: 1px;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-size: 1.2rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.5px;
  line-height: 1;
}

.brand-tag {
  font-size: 0.6rem;
  font-weight: 700;
  color: #f97316;
  letter-spacing: 1.2px;
  margin-top: 2px;
}

.nav-links {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: nowrap;
}

.nav-btn {
  background: none;
  border: none;
  color: #475569;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  padding: 0.5rem 0.75rem;
  border-radius: 8px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 0.35rem;
  white-space: nowrap;
}

.nav-btn:hover {
  color: #0284c7;
  background: #f1f5f9;
}

.btn-cart-nav {
  position: relative;
  color: #f97316;
  font-weight: 800;
}

.cart-badge {
  background: #f97316;
  color: white;
  font-size: 0.7rem;
  font-weight: 900;
  padding: 0.15rem 0.45rem;
  border-radius: 10px;
  line-height: 1;
}

.nav-btn-admin {
  color: #10b981;
  font-weight: 800;
}

.nav-btn-admin:hover {
  color: #059669;
  background: #ecfdf5;
}

.nav-btn-dashboard {
  color: #6366f1;
  font-weight: 800;
}

/* Notifications Bell & Popup Styling */
.notif-wrapper {
  position: relative;
}

.btn-notif-nav {
  position: relative;
  color: #6366f1;
  font-weight: 800;
}

.notif-badge {
  background: #ef4444;
  color: white;
  font-size: 0.68rem;
  font-weight: 900;
  padding: 0.15rem 0.45rem;
  border-radius: 10px;
  line-height: 1;
}

.notif-dropdown {
  position: absolute;
  top: 120%;
  right: 0;
  width: 360px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.15);
  z-index: 1000;
  overflow: hidden;
}

.notif-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.85rem 1rem;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  font-size: 0.9rem;
}

.btn-mark-all {
  background: none;
  border: none;
  color: #6366f1;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-mark-all:hover {
  text-decoration: underline;
}

.notif-list {
  max-height: 380px;
  overflow-y: auto;
}

.notif-item {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.15s ease;
}

.notif-item:hover {
  background: #f8fafc;
}

.notif-item.unread {
  background: #eef2ff;
  border-left: 4px solid #6366f1;
}

.notif-item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.notif-title {
  font-size: 0.85rem;
  font-weight: 800;
  color: #0f172a;
}

.notif-time {
  font-size: 0.72rem;
  color: #94a3b8;
  white-space: nowrap;
}

.notif-msg {
  font-size: 0.8rem;
  color: #475569;
  line-height: 1.35;
}

.empty-notif {
  padding: 2rem 1rem;
  text-align: center;
  color: #94a3b8;
  font-size: 0.85rem;
}

.nav-btn-dashboard:hover {
  color: #4f46e5;
  background: #eef2ff;
}
.auth-section {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.user-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 0.35rem 0.75rem;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.user-badge:hover {
  background: #e2e8f0;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #0284c7;
}

.username {
  color: #0f172a;
  font-size: 0.85rem;
  font-weight: 700;
  white-space: nowrap;
}

.role-tag {
  background: #10b981;
  color: white;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.15rem 0.45rem;
  border-radius: 6px;
}

/* User Hover Dropdown Menu Styling */
.user-dropdown-wrapper {
  position: relative;
  display: inline-block;
  padding-bottom: 6px; /* Bridge gap for smooth hover */
}

.dropdown-arrow {
  font-size: 0.75rem;
  color: #64748b;
  margin-left: 0.2rem;
  transition: transform 0.2s ease;
}

.user-dropdown-wrapper:hover .dropdown-arrow {
  transform: rotate(180deg);
}

.user-dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  width: 250px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.15);
  padding: 0.6rem;
  z-index: 1100;
  display: none;
}

.user-dropdown-wrapper:hover .user-dropdown-menu {
  display: block;
  animation: dropdownFadeIn 0.2s ease;
}

@keyframes dropdownFadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.user-menu-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.5rem 0.6rem 0.5rem;
}

.menu-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #0284c7;
}

.menu-user-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.menu-name {
  font-size: 0.9rem;
  font-weight: 800;
  color: #0f172a;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.menu-email {
  font-size: 0.75rem;
  color: #64748b;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.menu-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 0.4rem 0;
}

.menu-section-label {
  font-size: 0.68rem;
  font-weight: 800;
  color: #10b981;
  letter-spacing: 0.5px;
  padding: 0.4rem 0.6rem 0.2rem 0.6rem;
}

.user-menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.6rem 0.75rem;
  border: none;
  background: none;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 700;
  color: #334155;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
}

.user-menu-item:hover {
  background: #f8fafc;
  color: #0284c7;
}

.user-menu-item.admin-item:hover {
  background: #ecfdf5;
  color: #059669;
}

.user-menu-item.owner-item:hover {
  background: #fef3c7;
  color: #d97706;
}

.role-tag-owner {
  background: #d97706 !important;
  color: white !important;
}

.user-menu-item.logout-item {
  color: #ef4444;
}

.user-menu-item.logout-item:hover {
  background: #fef2f2;
  color: #dc2626;
}

.menu-icon {
  font-size: 1rem;
}

.btn-login {
  background: transparent;
  border: 1.5px solid #0284c7;
  color: #0284c7;
  padding: 0.45rem 1rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-login:hover {
  background: #0284c7;
  color: white;
}

.btn-register {
  background: linear-gradient(135deg, #f97316, #ea580c);
  border: none;
  color: white;
  padding: 0.5rem 1.1rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.25);
  transition: transform 0.2s ease;
  white-space: nowrap;
}

.btn-register:hover {
  transform: translateY(-1px);
}

.btn-logout {
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #ef4444;
  padding: 0.4rem 0.8rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-logout:hover {
  background: rgba(239, 68, 68, 0.15);
}
</style>

<!-- Feature Implementation: ui ô search có auto-suggest -->

<!-- Stage 2: Search with Auto-Suggest -->