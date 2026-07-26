<script setup>
import { ref, onMounted, computed } from 'vue';
import { publicProductApi, reviewApi } from '../services/api';
import { cartState } from '../state/cartStore';
import { authState } from '../state/authStore';

const props = defineProps({
  productId: {
    type: [Number, String],
    required: true
  }
});

const emit = defineEmits(['navigate', 'add-to-cart']);

const product = ref(null);
const loading = ref(true);
const errorMessage = ref('');

// Review & Rating State
const reviews = ref([]);
const ratingSummary = ref(null);

// Gallery
const activeImageUrl = ref('');

// Variant Selection
const selectedSize = ref('');
const selectedColor = ref('');
const buyQuantity = ref(1);
const addedSuccessMsg = ref('');

onMounted(async () => {
  await fetchProductDetail();
  await fetchReviewsAndSummary();
});

async function fetchProductDetail() {
  loading.value = true;
  errorMessage.value = '';
  try {
    let res;
    if (!isNaN(props.productId)) {
      try {
        res = await publicProductApi.getProductById(props.productId);
      } catch (e) {
        const searchRes = await publicProductApi.searchProducts(String(props.productId));
        if (searchRes.data && searchRes.data.length > 0) {
          res = { data: searchRes.data[0] };
        } else {
          throw e;
        }
      }
    } else {
      const searchRes = await publicProductApi.searchProducts(String(props.productId));
      if (searchRes.data && searchRes.data.length > 0) {
        res = { data: searchRes.data[0] };
      } else {
        throw new Error('Sản phẩm không tồn tại!');
      }
    }

    product.value = res.data;
    if (product.value) {
      activeImageUrl.value = product.value.thumbnailUrl || (product.value.imageUrls && product.value.imageUrls[0]) || '';
      
      // Auto select first available in-stock variant
      if (product.value.variants && product.value.variants.length) {
        const inStockVar = product.value.variants.find(v => v.stockQuantity > 0) || product.value.variants[0];
        selectedSize.value = inStockVar.size;
        selectedColor.value = inStockVar.color;
      }
    }
  } catch (err) {
    errorMessage.value = 'Sản phẩm này hiện không còn khả dụng hoặc đã ngưng kinh doanh.';
  } finally {
    loading.value = false;
  }
}

const replyingToReviewId = ref(null);
const replyTargetUserName = ref('');
const replyCommentText = ref('');
const replyLoading = ref(false);

async function fetchReviewsAndSummary() {
  try {
    const currentUserId = authState.user ? authState.user.id : null;
    const [revRes, sumRes] = await Promise.all([
      reviewApi.getProductReviews(props.productId, currentUserId),
      reviewApi.getProductSummary(props.productId),
    ]);
    reviews.value = revRes.data || [];
    ratingSummary.value = sumRes.data || null;
  } catch (err) {
    console.error('Lỗi tải đánh giá sản phẩm:', err);
  }
}

async function handleVote(targetReview, voteType) {
  if (!authState.user) {
    alert('Vui lòng đăng nhập để thích hoặc không thích bình luận này!');
    emit('navigate', 'login');
    return;
  }
  try {
    await reviewApi.voteReview(targetReview.id, authState.user.id, voteType);
    await fetchReviewsAndSummary();
  } catch (err) {
    console.error('Lỗi vote bình luận:', err);
  }
}

function toggleReplyBox(targetReview) {
  if (replyingToReviewId.value === targetReview.id) {
    replyingToReviewId.value = null;
    replyTargetUserName.value = '';
  } else {
    replyingToReviewId.value = targetReview.id;
    replyTargetUserName.value = targetReview.userName;
    replyCommentText.value = '';
  }
}

async function handleSendReply(targetReviewId) {
  if (!authState.user) {
    emit('navigate', 'login');
    return;
  }
  if (!replyCommentText.value.trim()) return;

  replyLoading.value = true;
  try {
    await reviewApi.createReview(authState.user.id, {
      productId: props.productId,
      parentId: targetReviewId,
      comment: replyCommentText.value.trim(),
    });
    replyingToReviewId.value = null;
    replyTargetUserName.value = '';
    replyCommentText.value = '';
    await fetchReviewsAndSummary();
  } catch (err) {
    alert(err.message || 'Gửi câu trả lời thất bại!');
  } finally {
    replyLoading.value = false;
  }
}

// Unique Size & Color lists
const availableSizes = computed(() => {
  if (!product.value || !product.value.variants) return [];
  return [...new Set(product.value.variants.map(v => v.size))];
});

const availableColors = computed(() => {
  if (!product.value || !product.value.variants) return [];
  return [...new Set(product.value.variants.map(v => v.color))];
});

// Find exact variant by combination
function getVariantByCombination(size, color) {
  if (!product.value || !product.value.variants) return null;
  return product.value.variants.find(v => v.size === size && v.color === color) || null;
}

// Current Selected Variant
const selectedVariant = computed(() => {
  return getVariantByCombination(selectedSize.value, selectedColor.value);
});

// Current Stock of Selected Variant
const currentStock = computed(() => {
  return selectedVariant.value ? selectedVariant.value.stockQuantity : 0;
});

// Check if combination exists and has stock
function isCombinationValid(size, color) {
  const v = getVariantByCombination(size, color);
  return v !== null;
}

function isCombinationInStock(size, color) {
  const v = getVariantByCombination(size, color);
  return v !== null && v.stockQuantity > 0;
}

// Smart Selection Handlers
function selectSize(size) {
  selectedSize.value = size;
  if (!isCombinationValid(size, selectedColor.value)) {
    const validVar = product.value.variants.find(v => v.size === size && v.stockQuantity > 0) 
      || product.value.variants.find(v => v.size === size);
    if (validVar) {
      selectedColor.value = validVar.color;
    }
  }
}

function selectColor(color) {
  selectedColor.value = color;
  if (!isCombinationValid(selectedSize.value, color)) {
    const validVar = product.value.variants.find(v => v.color === color && v.stockQuantity > 0) 
      || product.value.variants.find(v => v.color === color);
    if (validVar) {
      selectedSize.value = validVar.size;
    }
  }
}

function changeQuantity(delta) {
  const newQty = buyQuantity.value + delta;
  if (newQty >= 1 && newQty <= (currentStock.value || 1)) {
    buyQuantity.value = newQty;
  }
}

async function handleAddToCart(isBuyNow = false) {
  if (!authState.user) {
    if (confirm('Bạn cần đăng nhập để thêm sản phẩm vào giỏ hàng. Đăng nhập ngay?')) {
      emit('navigate', 'login');
    }
    return;
  }

  if (!selectedVariant.value) {
    alert('Biến thể Size & Màu sắc này không tồn tại!');
    return;
  }
  if (currentStock.value <= 0) {
    alert('Biến thể này hiện đã hết hàng!');
    return;
  }

  try {
    await cartState.addToCart(selectedVariant.value.id, buyQuantity.value);
    
    if (isBuyNow) {
      emit('navigate', 'cart');
    } else {
      addedSuccessMsg.value = `Đã thêm ${buyQuantity.value} sản phẩm "${product.value.name}" (${selectedSize.value} - ${selectedColor.value}) vào giỏ hàng!`;
      setTimeout(() => {
        addedSuccessMsg.value = '';
      }, 3500);
    }
  } catch (err) {
    alert(err.message || 'Không thể thêm sản phẩm vào giỏ hàng!');
  }
}

function formatPrice(val) {
  if (!val) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}

function calcDiscount(price, salePrice) {
  if (!price || !salePrice || salePrice >= price) return 0;
  return Math.round(((price - salePrice) / price) * 100);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('vi-VN');
}
</script>

<template>
  <div class="detail-page">
    <button class="btn-back" @click="emit('navigate', 'products')">◄ Quay Lại Danh Sách</button>

    <div v-if="loading" class="loading-box">
      <span class="spinner"></span> Đang tải thông tin chi tiết sản phẩm...
    </div>

    <div v-else-if="errorMessage" class="error-card-wrapper">
      <div class="error-card-content">
        <span class="error-icon-big">⚠️</span>
        <h3>{{ errorMessage }}</h3>
        <p>Sản phẩm này có thể đã được xóa hoặc chỉnh sửa thông tin. Quý khách vui lòng tham khảo các mẫu thời trang mới nhất của FS SHOP nhé!</p>
        <button class="btn-browse-catalog" @click="emit('navigate', 'products')">🛍️ XEM DANH SÁCH SẢN PHẨM KHÁC</button>
      </div>
    </div>

    <div v-else-if="product" class="detail-container">
      <!-- Image Gallery Left -->
      <div class="gallery-section">
        <div class="main-image-box">
          <img :src="activeImageUrl || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800'" class="main-img" />
          <span v-if="calcDiscount(product.price, product.salePrice) > 0" class="badge-discount">
            -{{ calcDiscount(product.price, product.salePrice) }}%
          </span>
        </div>

        <div v-if="product.imageUrls && product.imageUrls.length > 1" class="thumb-list">
          <img
            v-for="(img, idx) in product.imageUrls"
            :key="idx"
            :src="img"
            :class="['thumb-item', activeImageUrl === img ? 'active' : '']"
            @click="activeImageUrl = img"
          />
        </div>
      </div>

      <!-- Product Info Right -->
      <div class="info-section">
        <div class="brand-tag">FS SHOP • {{ product.categoryName || 'BỘ SƯU TẬP THỜI TRANG' }}</div>
        <h1 class="product-title">{{ product.name }}</h1>
        <div class="sku-row">
          <span>Mã SKU: <code>{{ product.sku }}</code></span>
          <span class="rating-stars">
            ⭐ <strong>{{ ratingSummary ? ratingSummary.averageRating : '5.0' }}</strong> / 5.0
            <span class="text-gray">({{ ratingSummary ? ratingSummary.totalReviews : 0 }} đánh giá)</span>
          </span>
        </div>

        <div class="price-box">
          <span class="sale-price">{{ formatPrice(product.salePrice || product.price) }}</span>
          <span v-if="product.salePrice && product.salePrice < product.price" class="old-price">
            {{ formatPrice(product.price) }}
          </span>
        </div>

        <!-- Notification Success -->
        <div v-if="addedSuccessMsg" class="alert-added">
          ✅ {{ addedSuccessMsg }}
        </div>

        <!-- Size Options -->
        <div class="option-group">
          <label>Kích Thước (Size): <strong class="text-blue">{{ selectedSize }}</strong></label>
          <div class="size-list">
            <button
              v-for="s in availableSizes"
              :key="s"
              :class="[
                'size-chip',
                selectedSize === s ? 'active' : '',
                !isCombinationValid(s, selectedColor) ? 'disabled-combination' : ''
              ]"
              @click="selectSize(s)"
            >
              {{ s }}
            </button>
          </div>
        </div>

        <!-- Color Options -->
        <div class="option-group">
          <label>Màu Sắc: <strong class="text-orange">{{ selectedColor }}</strong></label>
          <div class="color-list">
            <button
              v-for="c in availableColors"
              :key="c"
              :class="[
                'color-chip',
                selectedColor === c ? 'active' : '',
                !isCombinationValid(selectedSize, c) ? 'disabled-combination' : ''
              ]"
              @click="selectColor(c)"
            >
              {{ c }}
            </button>
          </div>
        </div>

        <!-- Stock Status -->
        <div class="stock-status-row">
          <span class="font-bold">Tình Trạng Kho:</span>
          <template v-if="selectedVariant">
            <span v-if="currentStock > 5" class="badge-stock stock-ok">Còn hàng ({{ currentStock }} SP có sẵn)</span>
            <span v-else-if="currentStock > 0" class="badge-stock stock-low">⚠️ Sắp hết hàng (Còn {{ currentStock }} SP)</span>
            <span v-else class="badge-stock stock-out">❌ Hết hàng (0 SP)</span>
          </template>
          <template v-else>
            <span class="badge-stock stock-invalid">❌ Phối màu & Kích cỡ này không tồn tại</span>
          </template>
        </div>

        <!-- Quantity Picker -->
        <div class="quantity-row">
          <label class="font-bold">Số Lượng:</label>
          <div class="quantity-picker">
            <button class="qty-btn" @click="changeQuantity(-1)" :disabled="buyQuantity <= 1 || currentStock <= 0">-</button>
            <span class="qty-val">{{ buyQuantity }}</span>
            <button class="qty-btn" @click="changeQuantity(1)" :disabled="buyQuantity >= currentStock || currentStock <= 0">+</button>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="action-row">
          <button class="btn-add-cart" @click="handleAddToCart(false)" :disabled="!selectedVariant || currentStock <= 0">
            🛒 THÊM VÀO GIỎ HÀNG
          </button>
          <button class="btn-buy-now" @click="handleAddToCart(true)" :disabled="!selectedVariant || currentStock <= 0">
            ⚡ MUA NGAY
          </button>
        </div>

        <!-- Product Description -->
        <div class="desc-box">
          <h3>Mô Tả Sản Phẩm</h3>
          <p>{{ product.description || 'Chất liệu vải mềm mại, thấm hút mồ hôi tốt, kiểu dáng trẻ trung thanh lịch chuẩn phong cách FS Shop.' }}</p>
        </div>
      </div>
    </div>

    <!-- Product Reviews Section (Sprint 8) -->
    <div v-if="product" class="reviews-section">
      <div class="reviews-header">
        <h2>⭐ Đánh Giá &amp; Nhận Xét Từng Khách Hàng ({{ reviews.length }})</h2>
      </div>

      <!-- Rating Summary Breakdown -->
      <div v-if="ratingSummary" class="rating-summary-card">
        <div class="rating-big-box">
          <div class="big-score">{{ ratingSummary.averageRating }}</div>
          <div class="big-stars">
            <span v-for="i in 5" :key="i" class="star-gold">★</span>
          </div>
          <div class="total-text">{{ ratingSummary.totalReviews }} Đánh Giá Xác Thực</div>
        </div>

        <div class="rating-bars-box">
          <div class="bar-row">
            <span>5 Star ★</span>
            <div class="bar-bg"><div class="bar-fill" :style="{ width: (ratingSummary.totalReviews ? (ratingSummary.star5Count / ratingSummary.totalReviews * 100) : 0) + '%' }"></div></div>
            <span>{{ ratingSummary.star5Count }}</span>
          </div>
          <div class="bar-row">
            <span>4 Star ★</span>
            <div class="bar-bg"><div class="bar-fill" :style="{ width: (ratingSummary.totalReviews ? (ratingSummary.star4Count / ratingSummary.totalReviews * 100) : 0) + '%' }"></div></div>
            <span>{{ ratingSummary.star4Count }}</span>
          </div>
          <div class="bar-row">
            <span>3 Star ★</span>
            <div class="bar-bg"><div class="bar-fill" :style="{ width: (ratingSummary.totalReviews ? (ratingSummary.star3Count / ratingSummary.totalReviews * 100) : 0) + '%' }"></div></div>
            <span>{{ ratingSummary.star3Count }}</span>
          </div>
          <div class="bar-row">
            <span>2 Star ★</span>
            <div class="bar-bg"><div class="bar-fill" :style="{ width: (ratingSummary.totalReviews ? (ratingSummary.star2Count / ratingSummary.totalReviews * 100) : 0) + '%' }"></div></div>
            <span>{{ ratingSummary.star2Count }}</span>
          </div>
          <div class="bar-row">
            <span>1 Star ★</span>
            <div class="bar-bg"><div class="bar-fill" :style="{ width: (ratingSummary.totalReviews ? (ratingSummary.star1Count / ratingSummary.totalReviews * 100) : 0) + '%' }"></div></div>
            <span>{{ ratingSummary.star1Count }}</span>
          </div>
        </div>
      </div>

      <!-- Reviews List -->
      <div v-if="!reviews.length" class="empty-reviews">
        <span>💬 Chưa có đánh giá nào cho sản phẩm này. Đơn hàng hoàn tất sẽ có quyền Đánh Giá!</span>
      </div>

      <div v-else class="reviews-list">
        <div v-for="rev in reviews" :key="rev.id" class="review-card">
          <!-- Root Review Header -->
          <div class="rev-header">
            <img :src="rev.userAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'" class="rev-avatar" />
            <div class="rev-user-meta">
              <div class="rev-user-name">{{ rev.userName }}</div>
              <div class="rev-stars" v-if="rev.ratingStars">
                <span v-for="s in rev.ratingStars" :key="s" class="star-gold">★</span>
                <span class="rev-date">• {{ formatDate(rev.createdAt) }}</span>
              </div>
              <div class="rev-date" v-else>{{ formatDate(rev.createdAt) }}</div>
            </div>
            <span class="badge-verified">✓ Đã Mua Hàng Thực Tế</span>
          </div>

          <!-- Root Review Content -->
          <p class="rev-comment">{{ rev.comment || 'Khách hàng không để lại nhận xét thêm.' }}</p>

          <!-- Actions Bar: Like, Dislike, Reply -->
          <div class="rev-actions-bar">
            <button 
              :class="['btn-vote', rev.userVote === 'LIKE' ? 'voted-like' : '']"
              @click="handleVote(rev, 'LIKE')"
            >
              👍 {{ rev.likeCount || 0 }}
            </button>

            <button 
              :class="['btn-vote', rev.userVote === 'DISLIKE' ? 'voted-dislike' : '']"
              @click="handleVote(rev, 'DISLIKE')"
            >
              👎 {{ rev.dislikeCount || 0 }}
            </button>

            <button class="btn-reply-toggle" @click="toggleReplyBox(rev)">
              💬 Trả Lời {{ rev.replies && rev.replies.length ? `(${rev.replies.length})` : '' }}
            </button>
          </div>

          <!-- In-line Reply Box for Root Review -->
          <div v-if="replyingToReviewId === rev.id" class="reply-input-box">
            <template v-if="authState.user">
              <div class="reply-target-hint">
                Đang trả lời <strong>@{{ replyTargetUserName }}</strong>
              </div>
              <textarea 
                v-model="replyCommentText" 
                :placeholder="`Trả lời @${replyTargetUserName}...`" 
                rows="2"
                class="reply-textarea"
              ></textarea>
              <div class="reply-actions">
                <button class="btn-cancel-reply" @click="replyingToReviewId = null">Hủy</button>
                <button 
                  class="btn-send-reply" 
                  @click="handleSendReply(rev.id)" 
                  :disabled="replyLoading || !replyCommentText.trim()"
                >
                  {{ replyLoading ? '...' : 'Gửi Phản Hồi' }}
                </button>
              </div>
            </template>
            <template v-else>
              <div class="reply-login-hint">
                Vui lòng <a href="#" @click.prevent="emit('navigate', 'login')">Đăng Nhập</a> để trả lời bình luận này.
              </div>
            </template>
          </div>

          <!-- Sub-comments / Nested Replies Thread -->
          <div v-if="rev.replies && rev.replies.length > 0" class="replies-thread">
            <div v-for="reply in rev.replies" :key="reply.id" class="reply-card">
              <div class="rev-header">
                <img :src="reply.userAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'" class="reply-avatar" />
                <div class="rev-user-meta">
                  <div class="rev-user-name">
                    {{ reply.userName }}
                    <span v-if="reply.replyToUserName" class="reply-target-tag">➔ @{{ reply.replyToUserName }}</span>
                    <span v-if="reply.userId === rev.userId" class="badge-author">Người mua</span>
                  </div>
                  <div class="rev-date">{{ formatDate(reply.createdAt) }}</div>
                </div>
              </div>

              <p class="reply-comment">{{ reply.comment }}</p>

              <div class="rev-actions-bar mini">
                <button 
                  :class="['btn-vote mini', reply.userVote === 'LIKE' ? 'voted-like' : '']"
                  @click="handleVote(reply, 'LIKE')"
                >
                  👍 {{ reply.likeCount || 0 }}
                </button>

                <button 
                  :class="['btn-vote mini', reply.userVote === 'DISLIKE' ? 'voted-dislike' : '']"
                  @click="handleVote(reply, 'DISLIKE')"
                >
                  👎 {{ reply.dislikeCount || 0 }}
                </button>

                <button class="btn-reply-toggle mini" @click="toggleReplyBox(reply)">
                  💬 Trả Lời
                </button>
              </div>

              <!-- In-line Reply Box for Sub-comment Reply -->
              <div v-if="replyingToReviewId === reply.id" class="reply-input-box">
                <template v-if="authState.user">
                  <div class="reply-target-hint">
                    Đang trả lời <strong>@{{ replyTargetUserName }}</strong>
                  </div>
                  <textarea 
                    v-model="replyCommentText" 
                    :placeholder="`Trả lời @${replyTargetUserName}...`" 
                    rows="2"
                    class="reply-textarea"
                  ></textarea>
                  <div class="reply-actions">
                    <button class="btn-cancel-reply" @click="replyingToReviewId = null">Hủy</button>
                    <button 
                      class="btn-send-reply" 
                      @click="handleSendReply(reply.id)" 
                      :disabled="replyLoading || !replyCommentText.trim()"
                    >
                      {{ replyLoading ? '...' : 'Gửi Phản Hồi' }}
                    </button>
                  </div>
                </template>
                <template v-else>
                  <div class="reply-login-hint">
                    Vui lòng <a href="#" @click.prevent="emit('navigate', 'login')">Đăng Nhập</a> để trả lời bình luận này.
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1.5rem 4rem 1.5rem;
}

.btn-back {
  background: white;
  border: 1px solid #cbd5e1;
  color: #0f172a;
  padding: 0.5rem 1.25rem;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  margin-bottom: 2rem;
}

.detail-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 3.5rem;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 2.5rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
}

@media (max-width: 850px) {
  .detail-container {
    grid-template-columns: 1fr;
    gap: 2rem;
    padding: 1.5rem;
  }
}

/* Gallery Left */
.gallery-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.main-image-box {
  position: relative;
  width: 100%;
  height: 420px;
  border-radius: 16px;
  overflow: hidden;
  background: #f8fafc;
}

.main-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.badge-discount {
  position: absolute;
  top: 1rem;
  left: 1rem;
  background: #ef4444;
  color: white;
  font-weight: 800;
  font-size: 0.85rem;
  padding: 0.3rem 0.8rem;
  border-radius: 8px;
}

.thumb-list {
  display: flex;
  gap: 0.75rem;
  overflow-x: auto;
}

.thumb-item {
  width: 70px;
  height: 70px;
  border-radius: 10px;
  object-fit: cover;
  border: 2px solid #e2e8f0;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.thumb-item.active {
  border-color: #0284c7;
}

/* Product Info Right */
.info-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.brand-tag {
  color: #0284c7;
  font-size: 0.8rem;
  font-weight: 800;
  letter-spacing: 1px;
}

.product-title {
  font-size: 2rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.25;
}

.sku-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  color: #64748b;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f1f5f9;
}

.rating-stars {
  color: #d97706;
  font-weight: 700;
}

.price-box {
  display: flex;
  align-items: baseline;
  gap: 1rem;
}

.sale-price {
  font-size: 2rem;
  font-weight: 800;
  color: #f97316;
}

.old-price {
  font-size: 1.1rem;
  color: #94a3b8;
  text-decoration: line-through;
}

.alert-added {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #16a34a;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.9rem;
}

.option-group label {
  display: block;
  font-size: 0.9rem;
  font-weight: 700;
  color: #334155;
  margin-bottom: 0.5rem;
}

.size-list, .color-list {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.size-chip, .color-chip {
  background: #f8fafc;
  border: 1.5px solid #cbd5e1;
  color: #0f172a;
  padding: 0.5rem 1rem;
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.size-chip.active, .color-chip.active {
  background: #0284c7;
  color: white;
  border-color: #0284c7;
}

.disabled-combination {
  opacity: 0.4;
  border-style: dashed;
}

.stock-status-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.9rem;
}

.badge-stock {
  font-weight: 700;
  padding: 0.25rem 0.65rem;
  border-radius: 8px;
  font-size: 0.8rem;
}

.stock-ok { background: #dcfce7; color: #15803d; }
.stock-low { background: #fef3c7; color: #b45309; }
.stock-out { background: #fee2e2; color: #b91c1c; }
.stock-invalid { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }

.quantity-row {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.quantity-picker {
  display: flex;
  align-items: center;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  overflow: hidden;
  background: #f8fafc;
}

.qty-btn {
  background: none;
  border: none;
  width: 38px;
  height: 38px;
  font-size: 1.1rem;
  font-weight: 700;
  cursor: pointer;
}

.qty-val {
  padding: 0 1rem;
  font-weight: 800;
  font-size: 1rem;
}

.action-row {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.btn-add-cart {
  flex: 1;
  background: #f1f5f9;
  border: 2px solid #0284c7;
  color: #0284c7;
  padding: 1rem;
  border-radius: 12px;
  font-weight: 800;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-add-cart:hover:not(:disabled) {
  background: #0284c7;
  color: white;
}

.btn-buy-now {
  flex: 1;
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: white;
  border: none;
  padding: 1rem;
  border-radius: 12px;
  font-weight: 800;
  font-size: 0.95rem;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(249, 115, 22, 0.3);
}

.btn-add-cart:disabled, .btn-buy-now:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.desc-box {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #f1f5f9;
}

.desc-box h3 {
  font-size: 1rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 0.5rem;
}

.desc-box p {
  font-size: 0.9rem;
  color: #475569;
  line-height: 1.6;
}

/* Reviews Section (Sprint 8) */
.reviews-section {
  margin-top: 3rem;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 2.5rem;
}

.reviews-header h2 {
  font-size: 1.4rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 1.5rem;
}

.rating-summary-card {
  display: flex;
  align-items: center;
  gap: 3rem;
  background: #f8fafc;
  border-radius: 16px;
  padding: 1.5rem 2rem;
  margin-bottom: 2rem;
}

.rating-big-box {
  text-align: center;
}

.big-score {
  font-size: 3rem;
  font-weight: 900;
  color: #0f172a;
  line-height: 1;
}

.star-gold {
  color: #f59e0b;
}

.big-stars {
  font-size: 1.25rem;
  margin: 0.25rem 0;
}

.total-text {
  font-size: 0.8rem;
  color: #64748b;
  font-weight: 700;
}

.rating-bars-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.85rem;
  font-weight: 700;
  color: #475569;
}

.bar-bg {
  flex: 1;
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: #f59e0b;
  border-radius: 4px;
}

.empty-reviews {
  text-align: center;
  padding: 2.5rem;
  background: #f8fafc;
  border-radius: 16px;
  color: #64748b;
  font-size: 0.95rem;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.review-card {
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 1.25rem;
}

.rev-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  margin-bottom: 0.6rem;
}

.rev-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e2e8f0;
}

.rev-user-name {
  font-weight: 800;
  font-size: 0.95rem;
  color: #0f172a;
}

.reply-target-tag {
  color: #6366f1;
  font-size: 0.78rem;
  font-weight: 800;
  margin-left: 0.35rem;
}

.reply-target-hint {
  font-size: 0.75rem;
  color: #6366f1;
  margin-bottom: 0.35rem;
}

.rev-stars {
  font-size: 0.85rem;
}

.rev-date {
  color: #94a3b8;
  font-size: 0.75rem;
  margin-left: 0.4rem;
}

.badge-verified {
  margin-left: auto;
  background: #dcfce7;
  color: #15803d;
  font-size: 0.75rem;
  font-weight: 800;
  padding: 0.25rem 0.65rem;
  border-radius: 20px;
}

.rev-comment {
  font-size: 0.9rem;
  color: #334155;
  line-height: 1.5;
  padding-left: 3.1rem;
}

/* Actions Bar: Vote & Reply */
.rev-actions-bar {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-top: 0.6rem;
  padding-left: 3.1rem;
}

.rev-actions-bar.mini {
  padding-left: 2.6rem;
  margin-top: 0.4rem;
}

.btn-vote {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
  border-radius: 20px;
  padding: 0.25rem 0.65rem;
  font-size: 0.78rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.15s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.btn-vote:hover {
  background: #e2e8f0;
}

.btn-vote.mini {
  padding: 0.15rem 0.5rem;
  font-size: 0.72rem;
}

.voted-like {
  background: #e0f2fe !important;
  color: #0284c7 !important;
  border-color: #38bdf8 !important;
}

.voted-dislike {
  background: #fef2f2 !important;
  color: #dc2626 !important;
  border-color: #fca5a5 !important;
}

.btn-reply-toggle {
  background: none;
  border: none;
  color: #6366f1;
  font-size: 0.8rem;
  font-weight: 800;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
}

.btn-reply-toggle:hover {
  text-decoration: underline;
}

/* In-line Reply Input Box */
.reply-input-box {
  margin-top: 0.75rem;
  margin-left: 3.1rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 0.85rem;
}

.reply-textarea {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0.5rem 0.75rem;
  font-size: 0.85rem;
  outline: none;
  resize: vertical;
  font-family: inherit;
}

.reply-textarea:focus {
  border-color: #6366f1;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.btn-cancel-reply {
  background: none;
  border: 1px solid #cbd5e1;
  color: #64748b;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-send-reply {
  background: #6366f1;
  color: white;
  border: none;
  padding: 0.35rem 0.85rem;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 800;
  cursor: pointer;
}

.btn-send-reply:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.reply-login-hint {
  font-size: 0.8rem;
  color: #64748b;
}

.reply-login-hint a {
  color: #6366f1;
  font-weight: 800;
}

/* Nested Replies Thread */
.replies-thread {
  margin-top: 0.85rem;
  margin-left: 3.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  border-left: 2px solid #e2e8f0;
  padding-left: 1rem;
}

.reply-card {
  background: #f8fafc;
  border-radius: 12px;
  padding: 0.75rem;
  border: 1px solid #f1f5f9;
}

.reply-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1.5px solid #cbd5e1;
}

.reply-comment {
  font-size: 0.85rem;
  color: #334155;
  line-height: 1.4;
  padding-left: 2.6rem;
}

.badge-author {
  background: #fef3c7;
  color: #d97706;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  margin-left: 0.3rem;
}

.loading-box {
  text-align: center;
  padding: 4rem 1rem;
  background: white;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
}

.error-card-wrapper {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 4rem 2rem;
  text-align: center;
  box-shadow: 0 10px 30px rgba(0,0,0,0.03);
}

.error-card-content {
  max-width: 500px;
  margin: 0 auto;
}

.error-icon-big {
  font-size: 3.5rem;
  display: block;
  margin-bottom: 1rem;
}

.error-card-content h3 {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 0.5rem;
}

.error-card-content p {
  font-size: 0.9rem;
  color: #64748b;
  margin-bottom: 1.5rem;
}

.btn-browse-catalog {
  background: linear-gradient(135deg, #0284c7, #2563eb);
  color: white;
  border: none;
  padding: 0.85rem 1.75rem;
  border-radius: 12px;
  font-weight: 800;
  font-size: 0.9rem;
  cursor: pointer;
  transition: transform 0.15s ease;
}

.btn-browse-catalog:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(2, 132, 199, 0.3);
}
</style>

<!-- Feature Implementation: ui trang chi tiết sp (ảnh, mô tả) -->

<!-- Feature Implementation: ui nút add to cart, alert -->

<!-- Feature Implementation: ui form đánh giá (1-5 sao) -->

<!-- Feature Implementation: ui hiển thị đánh giá ct sp -->

<!-- Stage 2: Add to Cart Action with Toast Feedback -->