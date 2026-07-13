<script setup>
import { ref } from 'vue';
import Navbar from './components/Navbar.vue';
import HomeView from './views/HomeView.vue';
import LoginView from './views/LoginView.vue';
import RegisterView from './views/RegisterView.vue';
import ProfileView from './views/ProfileView.vue';
import AdminProductsView from './views/AdminProductsView.vue';
import AdminOrdersView from './views/AdminOrdersView.vue';
import AdminDashboardView from './views/AdminDashboardView.vue';
import AdminVouchersView from './views/AdminVouchersView.vue';
import AdminUsersView from './views/AdminUsersView.vue';
import ProductListView from './views/ProductListView.vue';
import ProductDetailView from './views/ProductDetailView.vue';
import CartView from './views/CartView.vue';
import CheckoutView from './views/CheckoutView.vue';
import OrderSuccessView from './views/OrderSuccessView.vue';
import OrdersView from './views/OrdersView.vue';

const currentView = ref('home');
const selectedProductId = ref(null);
const createdOrderData = ref(null);

function navigateTo(viewName, param) {
  if (viewName === 'product-detail' && param) {
    selectedProductId.value = param;
    currentView.value = 'product-detail';
  } else {
    currentView.value = viewName;
  }
}

function viewProductDetail(productId) {
  selectedProductId.value = productId;
  currentView.value = 'product-detail';
}

function handleOrderCompleted(orderData) {
  createdOrderData.value = orderData;
  currentView.value = 'order-success';
}
</script>

<template>
  <div class="app-wrapper">
    <Navbar @navigate="navigateTo" />

    <main class="main-content">
      <HomeView v-if="currentView === 'home'" @navigate="navigateTo" />
      <ProductListView v-else-if="currentView === 'products'" @navigate="navigateTo" @view-product="viewProductDetail" />
      <ProductDetailView v-else-if="currentView === 'product-detail'" :productId="selectedProductId" @navigate="navigateTo" />
      <CartView v-else-if="currentView === 'cart'" @navigate="navigateTo" />
      <CheckoutView v-else-if="currentView === 'checkout'" @navigate="navigateTo" @order-completed="handleOrderCompleted" />
      <OrderSuccessView v-else-if="currentView === 'order-success'" :order="createdOrderData" @navigate="navigateTo" />
      <OrdersView v-else-if="currentView === 'orders'" @navigate="navigateTo" />
      <AdminOrdersView v-else-if="currentView === 'admin-orders'" @navigate="navigateTo" />
      <AdminDashboardView v-else-if="currentView === 'admin-dashboard'" @navigate="navigateTo" />
      <AdminVouchersView v-else-if="currentView === 'admin-vouchers'" @navigate="navigateTo" />
      <AdminUsersView v-else-if="currentView === 'admin-users'" @navigate="navigateTo" />
      <LoginView v-else-if="currentView === 'login'" @navigate="navigateTo" />
      <RegisterView v-else-if="currentView === 'register'" @navigate="navigateTo" />
      <ProfileView v-else-if="currentView === 'profile'" @navigate="navigateTo" />
      <AdminProductsView v-else-if="currentView === 'admin-products'" @navigate="navigateTo" />
      <HomeView v-else @navigate="navigateTo" />
    </main>

    <footer class="app-footer">
      <div class="footer-container">
        <div class="footer-brand">
          <div class="footer-logo">FS SHOP (Fashion Store)</div>
          <p>Thương hiệu thời trang nam nữ cao cấp - Phong cách hiện đại, chất lượng vượt trội</p>
        </div>
        <div class="footer-copyright">
          © 2026 FS SHOP. All rights reserved.
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.app-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f8fafc;
}

.main-content {
  flex: 1;
}

.app-footer {
  background: white;
  border-top: 1px solid #e2e8f0;
  padding: 2rem 1.5rem;
  margin-top: 3rem;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.footer-logo {
  font-weight: 800;
  font-size: 1.1rem;
  color: #0f172a;
}

.footer-brand p {
  font-size: 0.85rem;
  color: #64748b;
}

.footer-copyright {
  font-size: 0.85rem;
  color: #94a3b8;
}
</style>
