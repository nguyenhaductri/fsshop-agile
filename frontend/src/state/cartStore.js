import { reactive } from 'vue';
import { cartApi } from '../services/api';
import { authState } from './authStore';

export const cartState = reactive({
  cartId: null,
  items: [],
  totalItems: 0,
  totalAmount: 0,
  loading: false,

  async fetchCart() {
    if (!authState.user) {
      this.items = [];
      this.totalItems = 0;
      this.totalAmount = 0;
      return;
    }

    this.loading = true;
    try {
      const res = await cartApi.getCart(authState.user.id);
      this.cartId = res.data.cartId;
      this.items = res.data.items || [];
      this.totalItems = res.data.totalItems || 0;
      this.totalAmount = res.data.totalAmount || 0;
    } catch (err) {
      console.error('Lỗi tải giỏ hàng:', err);
    } finally {
      this.loading = false;
    }
  },

  async addToCart(variantId, quantity = 1) {
    if (!authState.user) {
      throw new Error('Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng!');
    }

    this.loading = true;
    try {
      const res = await cartApi.addToCart(authState.user.id, { variantId, quantity });
      this.items = res.data.items || [];
      this.totalItems = res.data.totalItems || 0;
      this.totalAmount = res.data.totalAmount || 0;
      return res.message;
    } finally {
      this.loading = false;
    }
  },

  async updateQuantity(cartItemId, quantity) {
    if (!authState.user) return;

    this.loading = true;
    try {
      const res = await cartApi.updateCartItem(authState.user.id, cartItemId, { quantity });
      this.items = res.data.items || [];
      this.totalItems = res.data.totalItems || 0;
      this.totalAmount = res.data.totalAmount || 0;
    } finally {
      this.loading = false;
    }
  },

  async removeItem(cartItemId) {
    if (!authState.user) return;

    this.loading = true;
    try {
      const res = await cartApi.removeCartItem(authState.user.id, cartItemId);
      this.items = res.data.items || [];
      this.totalItems = res.data.totalItems || 0;
      this.totalAmount = res.data.totalAmount || 0;
    } finally {
      this.loading = false;
    }
  },

  async clearCart() {
    if (!authState.user) return;

    this.loading = true;
    try {
      await cartApi.clearCart(authState.user.id);
      this.items = [];
      this.totalItems = 0;
      this.totalAmount = 0;
    } finally {
      this.loading = false;
    }
  }
});
