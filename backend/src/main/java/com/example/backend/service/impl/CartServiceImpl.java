package com.example.backend.service.impl;

import com.example.backend.dto.request.AddToCartRequest;
import com.example.backend.dto.request.UpdateCartItemRequest;
import com.example.backend.dto.response.CartItemResponse;
import com.example.backend.dto.response.CartResponse;
import com.example.backend.entity.Cart;
import com.example.backend.entity.CartItem;
import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.User;
import com.example.backend.repository.CartItemRepository;
import com.example.backend.repository.CartRepository;
import com.example.backend.repository.ProductVariantRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CartResponse getCartByUser(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new RuntimeException("Biến thể sản phẩm không tồn tại!"));

        if (variant.getStockQuantity() <= 0) {
            throw new RuntimeException("Biến thể sản phẩm này hiện đã hết hàng!");
        }

        int addQty = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1;

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId());

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newTotalQty = existingItem.getQuantity() + addQty;
            if (newTotalQty > variant.getStockQuantity()) {
                throw new RuntimeException("Số lượng trong giỏ vượt quá tồn kho còn lại (" + variant.getStockQuantity() + " SP)!");
            }
            existingItem.setQuantity(newTotalQty);
            cartItemRepository.save(existingItem);
        } else {
            if (addQty > variant.getStockQuantity()) {
                throw new RuntimeException("Số lượng thêm vượt quá tồn kho còn lại (" + variant.getStockQuantity() + " SP)!");
            }
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(addQty)
                    .build();
            cartItemRepository.save(newItem);
        }

        return getCartByUser(userId);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng!"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Không có quyền chỉnh sửa sản phẩm giỏ hàng này!");
        }

        int newQty = request.getQuantity();
        if (newQty <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            ProductVariant variant = cartItem.getVariant();
            if (newQty > variant.getStockQuantity()) {
                throw new RuntimeException("Số lượng cập nhật vượt quá tồn kho khả dụng (" + variant.getStockQuantity() + " SP)!");
            }
            cartItem.setQuantity(newQty);
            cartItemRepository.save(cartItem);
        }

        return getCartByUser(userId);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng!"));

        if (cartItem.getCart().getId().equals(cart.getId())) {
            cartItemRepository.delete(cartItem);
        }

        return getCartByUser(userId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng!"));
            Cart newCart = Cart.builder()
                    .user(user)
                    .items(new ArrayList<>())
                    .build();
            return cartRepository.save(newCart);
        });
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItem item : items) {
            ProductVariant variant = item.getVariant();
            var product = variant.getProduct();

            BigDecimal price = product.getPrice();
            BigDecimal salePrice = product.getSalePrice();
            BigDecimal unitPrice = (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0) ? salePrice : price;

            BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subTotal);
            totalItems += item.getQuantity();

            String thumbnailUrl = (product.getImages() != null && !product.getImages().isEmpty())
                    ? product.getImages().get(0).getImageUrl()
                    : null;

            itemResponses.add(CartItemResponse.builder()
                    .id(item.getId())
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .thumbnailUrl(thumbnailUrl)
                    .variantId(variant.getId())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .price(price)
                    .salePrice(salePrice)
                    .unitPrice(unitPrice)
                    .quantity(item.getQuantity())
                    .subTotal(subTotal)
                    .availableStock(variant.getStockQuantity())
                    .build());
        }

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemResponses)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .build();
    }
}

// Feature Implementation: api update cart item
