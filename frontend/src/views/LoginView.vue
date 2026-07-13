<script setup>
import { ref } from 'vue';
import { authApi } from '../services/api';
import { authState } from '../state/authStore';

const emit = defineEmits(['navigate']);

const username = ref('');
const password = ref('');
const showPassword = ref(false);
const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

async function handleLogin() {
  if (!username.value || !password.value) {
    errorMessage.value = 'Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!';
    return;
  }

  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const res = await authApi.login({
      username: username.value,
      password: password.value,
    });

    successMessage.value = 'Đăng nhập thành công!';
    authState.setUser(res.data, res.data.token);

    setTimeout(() => {
      emit('navigate', 'home');
    }, 800);
  } catch (err) {
    errorMessage.value = err.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại!';
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
        <h2>Đăng Nhập Tài Khoản</h2>
        <p>Chào mừng bạn trở lại với hệ thống FS Shop</p>
      </div>

      <div v-if="errorMessage" class="alert alert-error">
        ⚠️ {{ errorMessage }}
      </div>

      <div v-if="successMessage" class="alert alert-success">
        ✅ {{ successMessage }}
      </div>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="form-group">
          <label>Tên đăng nhập / Email</label>
          <input
            v-model="username"
            type="text"
            placeholder="Nhập tên đăng nhập..."
            required
          />
        </div>

        <div class="form-group">
          <label>Mật khẩu</label>
          <div class="password-wrapper">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Nhập mật khẩu..."
              required
            />
            <button
              type="button"
              class="btn-toggle-eye"
              @click="showPassword = !showPassword"
            >
              {{ showPassword ? '👁️' : '🙈' }}
            </button>
          </div>
        </div>

        <button type="submit" class="btn-submit" :disabled="loading">
          <span v-if="loading">Đang đăng nhập...</span>
          <span v-else>ĐĂNG NHẬP NGAY</span>
        </button>
      </form>

      <div class="auth-footer">
        <span>Bạn chưa có tài khoản?</span>
        <button class="btn-link" @click="emit('navigate', 'register')">Tạo tài khoản mới</button>
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
  max-width: 440px;
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

.password-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-wrapper input {
  width: 100%;
  padding-right: 2.5rem;
}

.btn-toggle-eye {
  position: absolute;
  right: 0.75rem;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
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

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

.demo-accounts-box {
  margin-top: 1.5rem;
  padding: 0.85rem;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.demo-title {
  font-size: 0.78rem;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 0.5rem;
  text-align: center;
}

.demo-buttons {
  display: flex;
  gap: 0.5rem;
  justify-content: center;
}

.btn-demo {
  flex: 1;
  background: white;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 0.4rem 0.5rem;
  border-radius: 8px;
  font-size: 0.78rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.btn-demo:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-demo.active {
  background: #0f172a;
  color: white;
  border-color: #0f172a;
}
</style>

<!-- Feature Implementation: dựng giao diện trang đăng ký/đăng nhập -->
