<script setup>
import { ref } from 'vue';
import { authApi } from '../services/api';
import { authState } from '../state/authStore';

const emit = defineEmits(['navigate']);

const username = ref('');
const password = ref('');
const confirmPassword = ref('');
const email = ref('');
const fullName = ref('');
const phone = ref('');
const address = ref('');

const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

async function handleRegister() {
  if (!username.value || !password.value || !email.value) {
    errorMessage.value = 'Vui lòng điền đầy đủ các thông tin bắt buộc (*)!';
    return;
  }

  if (password.value !== confirmPassword.value) {
    errorMessage.value = 'Mật khẩu xác nhận không khớp!';
    return;
  }

  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const res = await authApi.register({
      username: username.value,
      password: password.value,
      email: email.value,
      fullName: fullName.value,
      phone: phone.value,
      address: address.value,
    });

    successMessage.value = 'Đăng ký thành công! Đang đăng nhập tự động...';
    authState.setUser(res.data, res.data.token);

    setTimeout(() => {
      emit('navigate', 'home');
    }, 1000);
  } catch (err) {
    errorMessage.value = err.message || 'Đăng ký thất bại!';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="brand-badge">FS SHOP</div>
        <h2>Tạo Tài Khoản Mới</h2>
        <p>Tham gia FS Shop để trải nghiệm mua sắm thời trang hàng đầu</p>
      </div>

      <div v-if="errorMessage" class="alert alert-error">
        ⚠️ {{ errorMessage }}
      </div>

      <div v-if="successMessage" class="alert alert-success">
        ✅ {{ successMessage }}
      </div>

      <form @submit.prevent="handleRegister" class="auth-form">
        <div class="form-row">
          <div class="form-group">
            <label>Tên đăng nhập *</label>
            <input v-model="username" type="text" placeholder="username..." required />
          </div>
          <div class="form-group">
            <label>Email *</label>
            <input v-model="email" type="email" placeholder="example@gmail.com" required />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>Mật khẩu *</label>
            <input v-model="password" type="password" placeholder="Mật khẩu..." required />
          </div>
          <div class="form-group">
            <label>Xác nhận mật khẩu *</label>
            <input v-model="confirmPassword" type="password" placeholder="Nhập lại mật khẩu..." required />
          </div>
        </div>

        <div class="form-group">
          <label>Họ và Tên</label>
          <input v-model="fullName" type="text" placeholder="Nguyễn Văn A..." />
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>Số điện thoại</label>
            <input v-model="phone" type="text" placeholder="0912..." />
          </div>
          <div class="form-group">
            <label>Địa chỉ nhận hàng</label>
            <input v-model="address" type="text" placeholder="Hà Nội / HCM..." />
          </div>
        </div>

        <button type="submit" class="btn-submit" :disabled="loading">
          <span v-if="loading">Đang đăng ký...</span>
          <span v-else>ĐĂNG KÝ TÀI KHOẢN</span>
        </button>
      </form>

      <div class="auth-footer">
        <span>Đã có tài khoản?</span>
        <button class="btn-link" @click="emit('navigate', 'login')">Đăng nhập ngay</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 80px);
  padding: 2.5rem 1rem;
}

.auth-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 2.5rem;
  width: 100%;
  max-width: 560px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.05);
}

.auth-header {
  text-align: center;
  margin-bottom: 2rem;
}

.brand-badge {
  display: inline-block;
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
  font-weight: 900;
  font-size: 0.75rem;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  letter-spacing: 1px;
  margin-bottom: 0.75rem;
}

.auth-header h2 {
  font-size: 1.75rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 0.4rem;
}

.auth-header p {
  color: #64748b;
  font-size: 0.9rem;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

@media (max-width: 500px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-group label {
  color: #334155;
  font-size: 0.85rem;
  font-weight: 700;
}

.form-group input {
  background: #f8fafc;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  padding: 0.8rem 1rem;
  color: #0f172a;
  font-size: 0.95rem;
  outline: none;
  transition: all 0.2s ease;
}

.form-group input:focus {
  border-color: #0284c7;
  background: white;
  box-shadow: 0 0 0 4px rgba(2, 132, 199, 0.1);
}

.btn-submit {
  background: linear-gradient(135deg, #f97316, #ea580c);
  border: none;
  color: white;
  padding: 0.9rem;
  border-radius: 10px;
  font-weight: 800;
  font-size: 0.95rem;
  cursor: pointer;
  margin-top: 0.5rem;
  box-shadow: 0 6px 15px rgba(249, 115, 22, 0.25);
  transition: transform 0.2s ease;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-1px);
}

.alert {
  padding: 0.75rem 1rem;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 1.25rem;
}

.alert-error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.alert-success {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #16a34a;
}

.auth-footer {
  text-align: center;
  margin-top: 1.75rem;
  font-size: 0.9rem;
  color: #64748b;
}

.btn-link {
  background: none;
  border: none;
  color: #0284c7;
  font-weight: 700;
  cursor: pointer;
  margin-left: 0.4rem;
}

.btn-link:hover {
  text-decoration: underline;
}
</style>

<!-- Feature Implementation: dựng giao diện trang đăng ký/đăng nhập -->
