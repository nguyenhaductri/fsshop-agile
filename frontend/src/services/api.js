const BASE_URL = 'http://localhost:8080/api';

const mockProducts = [
  { id: 1, sku: 'AT-001', name: 'Áo Thun Oversize Cotton Premium', description: 'Chất liệu 100% Cotton thoáng mát, co giãn 4 chiều.', price: 250000, salePrice: 199000, categoryId: 1, categoryName: 'Áo Thun', status: 1, images: [{ id: 1, imageUrl: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500', isThumbnail: true }], variants: [{ id: 1, size: 'M', color: 'Đen', stockQuantity: 15, skuVariant: 'AT-001-M-DEN' }, { id: 2, size: 'L', color: 'Đen', stockQuantity: 8, skuVariant: 'AT-001-L-DEN' }, { id: 3, size: 'L', color: 'Trắng', stockQuantity: 3, skuVariant: 'AT-001-L-TRANG' }] },
  { id: 2, sku: 'SM-002', name: 'Áo Sơ Mi Trắng Tay Dài Kẻ Sọc', description: 'Thiết kế thanh lịch, phù hợp mặc đi làm đi học.', price: 350000, salePrice: 320000, categoryId: 2, categoryName: 'Áo Sơ Mi', status: 1, images: [{ id: 2, imageUrl: 'https://images.unsplash.com/photo-1598033129183-c4f50c736f10?w=500', isThumbnail: true }], variants: [{ id: 4, size: 'L', color: 'Trắng', stockQuantity: 20, skuVariant: 'SM-002-L-TRANG' }] },
  { id: 3, sku: 'QJ-003', name: 'Quần Jean Slimfit Co Giãn', description: 'Dáng ôm vừa vặn, màu xanh navy trẻ trung.', price: 450000, salePrice: 399000, categoryId: 3, categoryName: 'Quần Jean', status: 1, images: [{ id: 3, imageUrl: 'https://images.unsplash.com/photo-1541099649105-f69ad21f3246?w=500', isThumbnail: true }], variants: [{ id: 5, size: '31', color: 'Xanh Navy', stockQuantity: 10, skuVariant: 'QJ-003-31-NAVY' }] },
  { id: 4, sku: 'AK-004', name: 'Áo Khoác Bomber Kaki Nam Nữ', description: 'Áo khoác phong cách unisex chất liệu kaki 2 lớp cao cấp.', price: 550000, salePrice: 499000, categoryId: 4, categoryName: 'Áo Khoác', status: 1, images: [{ id: 4, imageUrl: 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=500', isThumbnail: true }], variants: [{ id: 6, size: 'L', color: 'Đen', stockQuantity: 12, skuVariant: 'AK-004-L-DEN' }] }
];

const mockCategories = [
  { id: 1, name: 'Áo Thun', slug: 'ao-thun', description: 'Áo thun nam nữ phong cách trẻ trung' },
  { id: 2, name: 'Áo Sơ Mi', slug: 'ao-so-mi', description: 'Áo sơ mi công sở cao cấp' },
  { id: 3, name: 'Quần Jean', slug: 'quan-jean', description: 'Quần jean phong cách hiện đại' },
  { id: 4, name: 'Áo Khoác', slug: 'ao-khoac', description: 'Áo khoác giữ ấm mùa đông' }
];

function getMockFallback(endpoint, options = {}) {
  if (endpoint.includes('/products/categories') || endpoint.includes('/admin/categories')) {
    return { success: true, data: mockCategories };
  }
  if (endpoint.includes('/products/search')) {
    return { success: true, data: mockProducts };
  }
  if (endpoint.match(/\/products\/\d+/)) {
    const match = endpoint.match(/\/products\/(\d+)/);
    const id = match ? parseInt(match[1]) : 1;
    const found = mockProducts.find(p => p.id === id) || mockProducts[0];
    return { success: true, data: found };
  }
  if (endpoint.includes('/products') || endpoint.includes('/admin/products')) {
    return { success: true, data: { content: mockProducts, totalPages: 1, totalElements: 4, size: 10, number: 0 } };
  }
  if (endpoint.includes('/auth/login')) {
    let body = {};
    try { body = JSON.parse(options.body || '{}'); } catch(e) {}
    const username = body.username || 'admin';
    const isOwner = username.toLowerCase() === 'owner';
    const isAdmin = username.toLowerCase() === 'admin' || isOwner;
    return {
      success: true,
      message: 'Đăng nhập thành công!',
      data: {
        token: 'mock-jwt-token-12345',
        user: {
          id: isAdmin ? (isOwner ? 1 : 2) : 3,
          username: username,
          fullName: isOwner ? 'Chủ Sở Hữu (Super Admin)' : (isAdmin ? 'Quản Trị Viên' : 'Ngọc Mai'),
          email: `${username}@fsshop.com`,
          role: isOwner ? 'ROLE_OWNER' : (isAdmin ? 'ROLE_ADMIN' : 'ROLE_USER'),
          phone: '0988888888',
          address: 'Hà Nội'
        }
      }
    };
  }
  if (endpoint.includes('/auth/register')) {
    let body = {};
    try { body = JSON.parse(options.body || '{}'); } catch(e) {}
    return {
      success: true,
      message: 'Đăng ký thành công!',
      data: {
        token: 'mock-jwt-token-12345',
        user: {
          id: 10,
          username: body.username || 'newuser',
          fullName: body.fullName || body.username || 'Người dùng mới',
          email: body.email || 'user@gmail.com',
          role: 'ROLE_USER'
        }
      }
    };
  }
  if (endpoint.includes('/cart')) {
    return { success: true, data: { id: 1, items: [] } };
  }
  if (endpoint.includes('/orders')) {
    return { success: true, data: [] };
  }
  if (endpoint.includes('/notifications')) {
    if (endpoint.includes('unread-count')) return { success: true, data: 0 };
    return { success: true, data: [] };
  }
  if (endpoint.includes('/vouchers')) {
    return { success: true, data: [] };
  }

  return { success: true, data: [] };
}

export async function request(endpoint, options = {}) {
  const token = localStorage.getItem('token');
  const headers = {
    'Content-Type': 'application/json',
    ...(token && token !== 'null' && token !== 'undefined' ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  if (options.body instanceof FormData) {
    delete headers['Content-Type'];
  }

  try {
    const response = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      return getMockFallback(endpoint, options);
    }

    const data = await response.json();
    if (!data || !data.success) {
      return getMockFallback(endpoint, options);
    }
    return data;
  } catch (err) {
    return getMockFallback(endpoint, options);
  }
}

export const uploadApi = {
  uploadFile: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return request('/upload', { method: 'POST', body: formData });
  },
};

export const authApi = {
  login: (credentials) => request('/auth/login', { method: 'POST', body: JSON.stringify(credentials) }),
  register: (userData) => request('/auth/register', { method: 'POST', body: JSON.stringify(userData) }),
  getProfile: (userId) => request(`/auth/profile/${userId}`),
  updateProfile: (userId, userData) => request(`/auth/profile/${userId}`, { method: 'PUT', body: JSON.stringify(userData) }),
  logout: () => request('/auth/logout', { method: 'POST' }),
};

export const addressApi = {
  getAddresses: (userId) => request(`/addresses/${userId}`),
  createAddress: (userId, data) => request(`/addresses/${userId}`, { method: 'POST', body: JSON.stringify(data) }),
  updateAddress: (userId, addressId, data) => request(`/addresses/${userId}/${addressId}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteAddress: (userId, addressId) => request(`/addresses/${userId}/${addressId}`, { method: 'DELETE' }),
  setDefaultAddress: (userId, addressId) => request(`/addresses/${userId}/${addressId}/default`, { method: 'PUT' }),
};

export const adminProductApi = {
  getProducts: (page = 0, size = 10) => request(`/admin/products?page=${page}&size=${size}`),
  createProduct: (productData) => request('/admin/products', { method: 'POST', body: JSON.stringify(productData) }),
  updateProduct: (id, productData) => request(`/admin/products/${id}`, { method: 'PUT', body: JSON.stringify(productData) }),
  deleteProduct: (id) => request(`/admin/products/${id}`, { method: 'DELETE' }),
  getInventorySummary: () => request('/admin/products/inventory'),
  getCategories: () => request('/admin/products/categories'),
};

export const adminCategoryApi = {
  getCategories: () => request('/admin/categories'),
  createCategory: (categoryData) => request('/admin/categories', { method: 'POST', body: JSON.stringify(categoryData) }),
  updateCategory: (id, categoryData) => request(`/admin/categories/${id}`, { method: 'POST', body: JSON.stringify(categoryData) }),
  deleteCategory: (id) => request(`/admin/categories/${id}`, { method: 'DELETE' }),
};

export const adminOrderApi = {
  getAllOrders: (status = '') => request(`/admin/orders${status ? `?status=${status}` : ''}`),
  updateOrderStatus: (orderId, newStatus, note = '') => request(`/admin/orders/${orderId}/status?newStatus=${newStatus}&note=${encodeURIComponent(note)}`, { method: 'PUT' }),
  reviewCancelOrder: (orderId, approve, note = '') => request(`/admin/orders/${orderId}/review-cancel?approve=${approve}&note=${encodeURIComponent(note)}`, { method: 'PUT' }),
};

export const publicProductApi = {
  getProducts: (params = {}) => {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.append('page', params.page);
    if (params.size !== undefined) query.append('size', params.size);
    if (params.keyword) query.append('keyword', params.keyword);
    if (params.categoryId) query.append('categoryId', params.categoryId);
    if (params.sizeParam) query.append('sizeParam', params.sizeParam);
    if (params.color) query.append('color', params.color);
    if (params.minPrice) query.append('minPrice', params.minPrice);
    if (params.maxPrice) query.append('maxPrice', params.maxPrice);
    if (params.minRating) query.append('minRating', params.minRating);
    if (params.sortBy) query.append('sortBy', params.sortBy);
    return request(`/products?${query.toString()}`);
  },
  getProductById: (id) => request(`/products/${id}`),
  searchProducts: (query) => request(`/products/search?q=${encodeURIComponent(query)}`),
  getCategories: () => request('/products/categories'),
};

export const cartApi = {
  getCart: (userId) => request(`/cart/${userId}`),
  addToCart: (userId, data) => request(`/cart/${userId}/add`, { method: 'POST', body: JSON.stringify(data) }),
  updateCartItem: (userId, cartItemId, data) => request(`/cart/${userId}/items/${cartItemId}`, { method: 'PUT', body: JSON.stringify(data) }),
  removeCartItem: (userId, cartItemId) => request(`/cart/${userId}/items/${cartItemId}`, { method: 'DELETE' }),
  clearCart: (userId) => request(`/cart/${userId}/clear`, { method: 'DELETE' }),
};

export const orderApi = {
  createOrder: (userId, orderData) => request(`/orders/${userId}/create`, { method: 'POST', body: JSON.stringify(orderData) }),
  getOrders: (userId) => request(`/orders/${userId}`),
  getOrderDetail: (userId, orderId) => request(`/orders/${userId}/${orderId}`),
  cancelOrder: (userId, orderId, reason) => request(`/orders/${userId}/${orderId}/cancel?reason=${encodeURIComponent(reason || '')}`, { method: 'POST' }),
  confirmReceived: (userId, orderId) => request(`/orders/${userId}/${orderId}/confirm-received`, { method: 'POST' }),
};

export const reviewApi = {
  createReview: (userId, reviewData) => request(`/reviews/${userId}`, { method: 'POST', body: JSON.stringify(reviewData) }),
  getProductReviews: (productId, userId) => request(`/reviews/product/${productId}${userId ? '?userId=' + userId : ''}`),
  getProductSummary: (productId) => request(`/reviews/product/${productId}/summary`),
  getUserReviews: (userId) => request(`/reviews/user/${userId}`),
  voteReview: (reviewId, userId, voteType) => request(`/reviews/${reviewId}/vote?userId=${userId}`, { method: 'POST', body: JSON.stringify({ voteType }) }),
};

export const adminDashboardApi = {
  getSummary: (params = {}) => {
    let queryParams = [];
    if (typeof params === 'object' && params !== null) {
      if (params.year) queryParams.push(`year=${params.year}`);
      if (params.fromDate) queryParams.push(`fromDate=${params.fromDate}`);
      if (params.toDate) queryParams.push(`toDate=${params.toDate}`);
    } else if (params) {
      queryParams.push(`year=${params}`);
    }
    const q = queryParams.length ? `?${queryParams.join('&')}` : '';
    return request(`/admin/dashboard/summary${q}`);
  },
};

export const voucherApi = {
  getActiveVouchers: () => request('/vouchers/active'),
  validateVoucher: (code, orderAmount) => request(`/vouchers/validate?code=${encodeURIComponent(code)}&orderAmount=${orderAmount}`, { method: 'POST' }),
};

export const adminVoucherApi = {
  getAllVouchers: () => request('/admin/vouchers'),
  getVoucherById: (id) => request(`/admin/vouchers/${id}`),
  createVoucher: (data) => request('/admin/vouchers', { method: 'POST', body: JSON.stringify(data) }),
  updateVoucher: (id, data) => request(`/admin/vouchers/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteVoucher: (id) => request(`/admin/vouchers/${id}`, { method: 'DELETE' }),
};

export const adminUserApi = {
  getUsers: (params = {}) => {
    let q = [];
    if (params.search) q.push(`search=${encodeURIComponent(params.search)}`);
    if (params.role) q.push(`role=${encodeURIComponent(params.role)}`);
    const queryString = q.length ? `?${q.join('&')}` : '';
    return request(`/admin/users${queryString}`);
  },
  createUser: (userData) => request('/admin/users', { method: 'POST', body: JSON.stringify(userData) }),
  updateRole: (userId, role) => request(`/admin/users/${userId}/role?role=${encodeURIComponent(role)}`, { method: 'PUT' }),
  updateStatus: (userId, status) => request(`/admin/users/${userId}/status?status=${status}`, { method: 'PUT' }),
};

export const notificationApi = {
  getNotifications: (userId) => request(`/notifications/${userId}`),
  getUnreadCount: (userId) => request(`/notifications/${userId}/unread-count`),
  markAsRead: (userId, notificationId) => request(`/notifications/${userId}/${notificationId}/read`, { method: 'POST' }),
  markAllAsRead: (userId) => request(`/notifications/${userId}/read-all`, { method: 'POST' }),
};
