<script setup>
import { ref, onMounted } from 'vue';
import { authApi, addressApi, uploadApi } from '../services/api';
import { authState } from '../state/authStore';

const emit = defineEmits(['navigate']);

const user = ref({});
const fullName = ref('');
const phone = ref('');
const address = ref('');
const avatar = ref('');

const avatarFileInputRef = ref(null);
const uploadingAvatar = ref(false);

async function handleAvatarUpload(event) {
  const file = event.target.files[0];
  if (!file) return;

  uploadingAvatar.value = true;
  try {
    const res = await uploadApi.uploadFile(file);
    avatar.value = res.data.fileUrl;
  } catch (err) {
    alert(err.message || 'Lỗi tải ảnh đại diện!');
  } finally {
    uploadingAvatar.value = false;
  }
}

// Address Book State
const addresses = ref([]);
const showAddressModal = ref(false);
const editingAddrId = ref(null);
const addrRecipientName = ref('');
const addrRecipientPhone = ref('');
const addrDetailAddress = ref('');
const addrIsDefault = ref(false);

const loading = ref(false);
const message = ref('');
const isError = ref(false);

onMounted(async () => {
  if (!authState.user) {
    emit('navigate', 'login');
    return;
  }
  await fetchProfile();
  await fetchAddresses();
});

async function fetchProfile() {
  try {
    const res = await authApi.getProfile(authState.user.id);
    user.value = res.data;
    fullName.value = res.data.fullName || '';
    phone.value = res.data.phone || '';
    address.value = res.data.address || '';
    avatar.value = res.data.avatar || '';
  } catch (err) {
    message.value = err.message || 'Không thể tải thông tin cá nhân!';
    isError.value = true;
  }
}

async function fetchAddresses() {
  try {
    const res = await addressApi.getAddresses(authState.user.id);
    addresses.value = res.data || [];
  } catch (err) {
    console.error('Lỗi tải sổ địa chỉ:', err);
  }
}

async function handleUpdateProfile() {
  loading.value = true;
  message.value = '';
  isError.value = false;

  try {
    const res = await authApi.updateProfile(authState.user.id, {
      fullName: fullName.value,
      phone: phone.value,
      address: address.value,
      avatar: avatar.value,
    });

    user.value = res.data;
    authState.setUser({
      ...authState.user,
      fullName: res.data.fullName,
      phone: res.data.phone,
      address: res.data.address,
      avatar: res.data.avatar,
    }, authState.token);

    message.value = 'Cập nhật thông tin cá nhân thành công!';
    await fetchAddresses();
  } catch (err) {
    message.value = err.message || 'Cập nhật thất bại!';
    isError.value = true;
  } finally {
    loading.value = false;
  }
}

function openAddAddressModal() {
  editingAddrId.value = null;
  addrRecipientName.value = fullName.value || '';
  addrRecipientPhone.value = phone.value || '';
  addrDetailAddress.value = '';
  addrIsDefault.value = addresses.value.length === 0;
  showAddressModal.value = true;
}

function openEditAddressModal(addr) {
  editingAddrId.value = addr.id;
  addrRecipientName.value = addr.recipientName;
  addrRecipientPhone.value = addr.recipientPhone;
  addrDetailAddress.value = addr.detailAddress || addr.fullAddress;
  addrIsDefault.value = addr.isDefault;
  showAddressModal.value = true;
}

async function handleSaveAddress() {
  if (!addrRecipientName.value || !addrRecipientPhone.value || !addrDetailAddress.value) {
    alert('Vui lòng điền đầy đủ thông tin địa chỉ!');
    return;
  }

  try {
    const payload = {
      recipientName: addrRecipientName.value,
      recipientPhone: addrRecipientPhone.value,
      detailAddress: addrDetailAddress.value,
      isDefault: addrIsDefault.value,
    };

    if (editingAddrId.value) {
      await addressApi.updateAddress(authState.user.id, editingAddrId.value, payload);
    } else {
      await addressApi.createAddress(authState.user.id, payload);
    }

    showAddressModal.value = false;
    await fetchAddresses();
  } catch (err) {
    alert(err.message || 'Lưu địa chỉ thất bại!');
  }
}

async function handleDeleteAddress(addrId) {
  if (confirm('Bạn có chắc muốn xóa địa chỉ này?')) {
    try {
      await addressApi.deleteAddress(authState.user.id, addrId);
      await fetchAddresses();
    } catch (err) {
      alert(err.message || 'Xóa địa chỉ thất bại!');
    }
  }
}

async function handleSetDefaultAddress(addrId) {
  try {
    await addressApi.setDefaultAddress(authState.user.id, addrId);
    await fetchAddresses();
  } catch (err) {
    alert(err.message || 'Đặt mặc định thất bại!');
  }
}
</script>

<template>
  <div class="profile-page">
    <div class="profile-layout">
      <!-- User Profile Card -->
      <div class="profile-card">
        <div class="profile-header">
          <div class="avatar-container">
            <img
              :src="avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200'"
              alt="Avatar"
              class="profile-avatar"
            />
          </div>
          <h2>{{ fullName || user.username }}</h2>
          <span class="user-role">{{ user.role === 'ROLE_ADMIN' ? 'Quản Trị Viên (Admin)' : 'Khách Hàng Thân Thiết' }}</span>
        </div>

        <div v-if="message" :class="['alert', isError ? 'alert-error' : 'alert-success']">
          {{ isError ? '⚠️' : '✅' }} {{ message }}
        </div>

        <form @submit.prevent="handleUpdateProfile" class="profile-form">
          <div class="form-row">
            <div class="form-group">
              <label>Tên đăng nhập (Cố định)</label>
              <input :value="user.username" type="text" disabled class="disabled-input" />
            </div>
            <div class="form-group">
              <label>Email (Cố định)</label>
              <input :value="user.email" type="text" disabled class="disabled-input" />
            </div>
          </div>

          <div class="form-group">
            <label>Họ và Tên</label>
            <input v-model="fullName" type="text" placeholder="Nhập họ và tên..." />
          </div>

          <div class="form-group">
            <label>Số điện thoại</label>
            <input v-model="phone" type="text" placeholder="Nhập số điện thoại..." />
          </div>

          <div class="form-group">
            <label>Địa chỉ giao hàng (Mặc định)</label>
            <input v-model="address" type="text" placeholder="Nhập địa chỉ giao hàng mặc định..." />
          </div>

          <div class="form-group">
            <label>Ảnh Đại Diện (Avatar)</label>
            <div class="file-upload-input-group">
              <input v-model="avatar" type="text" placeholder="Dán link ảnh hoặc tải file từ máy..." />
              <input 
                type="file" 
                ref="avatarFileInputRef" 
                accept="image/*" 
                style="display: none" 
                @change="handleAvatarUpload" 
              />
              <button 
                type="button" 
                class="btn-upload-file" 
                @click="avatarFileInputRef.click()" 
                :disabled="uploadingAvatar"
              >
                {{ uploadingAvatar ? '⏳ Đang tải...' : '📁 Chọn Ảnh Từ Máy' }}
              </button>
            </div>
            <!-- Image Preview Box -->
            <div v-if="avatar" class="avatar-preview-box">
              <span class="preview-label">Xem trước ảnh đại diện:</span>
              <img :src="avatar" alt="Avatar Preview" class="avatar-preview-img" />
            </div>
          </div>

          <button type="submit" class="btn-save" :disabled="loading">
            <span v-if="loading">Đang lưu thay đổi...</span>
            <span v-else>CẬP NHẬT THÔNG TIN</span>
          </button>
        </form>
      </div>

      <!-- Address Book Card -->
      <div class="address-book-card">
        <div class="address-header">
          <div>
            <h3>📍 Sổ Địa Chỉ Nhận Hàng</h3>
            <p>Quản lý các địa chỉ giao hàng để chọn nhanh khi mua sắm</p>
          </div>
          <button class="btn-add-address" @click="openAddAddressModal">
            ➕ Thêm Địa Chỉ Mới
          </button>
        </div>

        <div v-if="!addresses.length" class="empty-address">
          <span>📮 Bạn chưa tạo địa chỉ giao hàng nào trong Sổ địa chỉ.</span>
        </div>

        <div v-else class="address-list">
          <div v-for="addr in addresses" :key="addr.id" class="address-item">
            <div class="addr-info">
              <div class="addr-name">
                <strong>{{ addr.recipientName }}</strong>
                <span class="addr-phone">({{ addr.recipientPhone }})</span>
                <span v-if="addr.isDefault" class="badge-default">MẶC ĐỊNH</span>
              </div>
              <div class="addr-detail">{{ addr.fullAddress || addr.detailAddress }}</div>
            </div>

            <div class="addr-actions">
              <button v-if="!addr.isDefault" class="btn-action-default" @click="handleSetDefaultAddress(addr.id)">
                Đặt mặc định
              </button>
              <button class="btn-action-edit" @click="openEditAddressModal(addr)">✏️ Sửa</button>
              <button class="btn-action-delete" @click="handleDeleteAddress(addr.id)">🗑️ Xóa</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Address Modal (Create / Edit) -->
    <div v-if="showAddressModal" class="modal-backdrop" @click.self="showAddressModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>{{ editingAddrId ? '✏️ Cập Nhật Địa Chỉ' : '➕ Thêm Địa Chỉ Mới' }}</h3>
          <button class="btn-close" @click="showAddressModal = false">✖</button>
        </div>

        <form @submit.prevent="handleSaveAddress" class="modal-form">
          <div class="form-group">
            <label>Họ và Tên người nhận *</label>
            <input v-model="addrRecipientName" type="text" placeholder="Nguyễn Văn A..." required />
          </div>

          <div class="form-group">
            <label>Số điện thoại nhận hàng *</label>
            <input v-model="addrRecipientPhone" type="text" placeholder="0912345678..." required />
          </div>

          <div class="form-group">
            <label>Địa chỉ nhận hàng chi tiết *</label>
            <textarea v-model="addrDetailAddress" rows="3" placeholder="Số nhà, Tên đường, Phường/Xã, Quận/Huyện, Tỉnh/TP..." required></textarea>
          </div>

          <div class="checkbox-group">
            <label class="flex-checkbox">
              <input v-model="addrIsDefault" type="checkbox" />
              <span>Đặt làm địa chỉ nhận hàng mặc định</span>
            </label>
          </div>

          <div class="modal-actions">
            <button type="button" class="btn-cancel" @click="showAddressModal = false">Hủy</button>
            <button type="submit" class="btn-submit">LƯU ĐỊA CHỈ</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 3rem 1.5rem;
}

.profile-layout {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 2rem;
}

@media (max-width: 900px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}

.profile-card, .address-book-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
}

.profile-header {
  text-align: center;
  margin-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
}

.profile-avatar {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  border: 3px solid #0284c7;
  object-fit: cover;
}

.profile-header h2 {
  font-size: 1.4rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.user-role {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
  padding: 0.2rem 0.6rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 700;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-width: 0;
}

.form-group label {
  color: #334155;
  font-size: 0.85rem;
  font-weight: 700;
}

.form-group input, .form-group textarea {
  background: #f8fafc;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  padding: 0.75rem 0.9rem;
  color: #0f172a;
  font-size: 0.9rem;
  outline: none;
  width: 100%;
  box-sizing: border-box;
}

.disabled-input {
  opacity: 0.65;
  background: #f1f5f9;
  cursor: not-allowed;
}

.btn-save {
  background: linear-gradient(135deg, #f97316, #ea580c);
  border: none;
  color: white;
  padding: 0.85rem;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
  margin-top: 0.5rem;
}

/* Address Book Card Right */
.address-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.address-header h3 {
  font-size: 1.2rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 0.2rem;
}

.address-header p {
  font-size: 0.85rem;
  color: #64748b;
}

.btn-add-address {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  color: white;
  border: none;
  padding: 0.6rem 1rem;
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
}

.empty-address {
  background: #f8fafc;
  padding: 2rem;
  text-align: center;
  border-radius: 12px;
  color: #64748b;
  font-size: 0.9rem;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.address-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 1.1rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.addr-name {
  font-size: 0.95rem;
  color: #0f172a;
  margin-bottom: 0.3rem;
}

.addr-phone {
  color: #64748b;
  margin-left: 0.4rem;
}

.badge-default {
  background: #10b981;
  color: white;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.15rem 0.45rem;
  border-radius: 6px;
  margin-left: 0.5rem;
}

.addr-detail {
  font-size: 0.85rem;
  color: #475569;
}

.addr-actions {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.btn-action-default {
  background: white;
  border: 1px solid #cbd5e1;
  color: #475569;
  font-size: 0.75rem;
  padding: 0.3rem 0.6rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-action-edit {
  background: #e0f2fe;
  border: none;
  color: #0284c7;
  font-size: 0.75rem;
  padding: 0.3rem 0.6rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-action-delete {
  background: #ffe4e6;
  border: none;
  color: #e11d48;
  font-size: 0.75rem;
  padding: 0.3rem 0.6rem;
  border-radius: 6px;
  cursor: pointer;
}

/* Modal Styling */
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
  max-width: 480px;
  padding: 1.75rem;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
}

.modal-header h3 {
  font-size: 1.2rem;
  font-weight: 800;
  color: #0f172a;
}

.btn-close {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 1.1rem;
  cursor: pointer;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.flex-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: #334155;
  cursor: pointer;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.btn-cancel {
  background: #f1f5f9;
  border: none;
  color: #475569;
  padding: 0.75rem 1.25rem;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
}

.btn-submit {
  background: linear-gradient(135deg, #f97316, #ea580c);
  border: none;
  color: white;
  padding: 0.75rem 1.5rem;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
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
  padding: 0.75rem 1rem;
  border-radius: 12px;
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

.avatar-preview-box {
  margin-top: 0.6rem;
  display: flex;
  align-items: center;
  gap: 0.85rem;
  background: #f8fafc;
  padding: 0.6rem 0.85rem;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.preview-label {
  font-size: 0.78rem;
  font-weight: 700;
  color: #64748b;
}

.avatar-preview-img {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #cbd5e1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.alert {
  padding: 0.75rem 1rem;
  border-radius: 10px;
  font-size: 0.85rem;
  margin-bottom: 1rem;
}
.alert-error { background: #fef2f2; border: 1px solid #fecaca; color: #dc2626; }
.alert-success { background: #f0fdf4; border: 1px solid #bbf7d0; color: #16a34a; }
</style>

<!-- Feature Implementation: thiết kế ui trang profile -->

<!-- Feature Implementation: phát triển api và ui quản lý sổ địa chỉ nhận hàng -->
