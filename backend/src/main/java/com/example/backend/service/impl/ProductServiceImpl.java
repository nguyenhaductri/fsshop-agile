package com.example.backend.service.impl;

import com.example.backend.dto.request.ProductRequest;
import com.example.backend.dto.request.ProductVariantRequest;
import com.example.backend.dto.response.*;
import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductImage;
import com.example.backend.entity.ProductVariant;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductVariantRepository;
import com.example.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.backend.repository.OrderItemRepository;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderItemRepository orderItemRepository;
    private final com.example.backend.repository.ReviewRepository reviewRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String sku = request.getSku();
        if (sku == null || sku.trim().isEmpty()) {
            long count = productRepository.count() + 1;
            sku = String.format("SP%03d", count);
            while (productRepository.existsBySku(sku)) {
                count++;
                sku = String.format("SP%03d", count);
            }
        } else if (productRepository.existsBySku(sku)) {
            throw new RuntimeException("Mã SKU sản phẩm đã tồn tại!");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElse(null);
        }

        Product product = Product.builder()
                .sku(sku)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .salePrice(request.getSalePrice())
                .category(category)
                .status(1)
                .images(new ArrayList<>())
                .variants(new ArrayList<>())
                .build();

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(request.getImageUrls().get(i))
                        .isThumbnail(i == 0)
                        .build();
                product.getImages().add(image);
            }
        }

        if (request.getVariants() != null) {
            for (ProductVariantRequest vr : request.getVariants()) {
                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .size(vr.getSize())
                        .color(vr.getColor())
                        .stockQuantity(vr.getStockQuantity() != null ? vr.getStockQuantity() : 0)
                        .skuVariant(vr.getSkuVariant() != null ? vr.getSkuVariant() : request.getSku() + "-" + vr.getSize() + "-" + vr.getColor())
                        .build();
                product.getVariants().add(variant);
            }
        }

        Product savedProduct = productRepository.save(product);
        return mapToProductResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm id: " + id));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getSalePrice() != null) product.setSalePrice(request.getSalePrice());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId()).orElse(null);
            product.setCategory(category);
        }

        if (request.getImageUrls() != null) {
            product.getImages().clear();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(request.getImageUrls().get(i))
                        .isThumbnail(i == 0)
                        .build();
                product.getImages().add(image);
            }
        }

        if (request.getVariants() != null) {
            List<ProductVariant> existingVariants = product.getVariants();
            List<ProductVariant> updatedVariantsList = new ArrayList<>();

            for (ProductVariantRequest vr : request.getVariants()) {
                ProductVariant existing = existingVariants.stream()
                        .filter(v -> (vr.getId() != null && vr.getId().equals(v.getId())) ||
                                     (v.getSize() != null && v.getSize().equalsIgnoreCase(vr.getSize()) &&
                                      v.getColor() != null && v.getColor().equalsIgnoreCase(vr.getColor())))
                        .findFirst()
                        .orElse(null);

                if (existing != null) {
                    existing.setSize(vr.getSize());
                    existing.setColor(vr.getColor());
                    existing.setStockQuantity(vr.getStockQuantity() != null ? vr.getStockQuantity() : 0);
                    if (vr.getSkuVariant() != null) {
                        existing.setSkuVariant(vr.getSkuVariant());
                    }
                    updatedVariantsList.add(existing);
                } else {
                    ProductVariant newVariant = ProductVariant.builder()
                            .product(product)
                            .size(vr.getSize())
                            .color(vr.getColor())
                            .stockQuantity(vr.getStockQuantity() != null ? vr.getStockQuantity() : 0)
                            .skuVariant(vr.getSkuVariant() != null ? vr.getSkuVariant() : product.getSku() + "-" + vr.getSize() + "-" + vr.getColor())
                            .build();
                    updatedVariantsList.add(newVariant);
                }
            }

            List<ProductVariant> toRemove = existingVariants.stream()
                    .filter(v -> !updatedVariantsList.contains(v))
                    .collect(Collectors.toList());

            for (ProductVariant v : toRemove) {
                if (v.getId() != null) {
                    orderItemRepository.unlinkVariant(v.getId());
                }
            }

            existingVariants.clear();
            existingVariants.addAll(updatedVariantsList);
        }

        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProductSoft(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));
        product.setStatus(0); // US07: Xóa mềm
        productRepository.save(product);
    }

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findByStatus(1, pageable)
                .map(this::mapToProductResponse);
    }

    @Override
    public Page<ProductResponse> filterProducts(String keyword, Long categoryId, String size, String color, BigDecimal minPrice, BigDecimal maxPrice, Double minRating, Pageable pageable) {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) <= 0) {
            minPrice = null;
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) <= 0) {
            maxPrice = null;
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal temp = minPrice;
            minPrice = maxPrice;
            maxPrice = temp;
        }

        boolean hasFilter = (keyword != null && !keyword.trim().isEmpty())
                || categoryId != null
                || (size != null && !size.trim().isEmpty())
                || (color != null && !color.trim().isEmpty())
                || minPrice != null
                || maxPrice != null;

        Page<ProductResponse> pageRes;
        if (!hasFilter) {
            pageRes = getAllProducts(pageable);
        } else {
            pageRes = productRepository.filterProducts(keyword, categoryId, size, color, minPrice, maxPrice, pageable)
                    .map(this::mapToProductResponse);
        }

        if (minRating != null && minRating > 0) {
            List<ProductResponse> filtered = pageRes.getContent().stream()
                    .filter(p -> p.getAverageRating() != null && p.getAverageRating() >= minRating)
                    .collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(filtered, pageable, pageRes.getTotalElements());
        }

        return pageRes;
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdAndStatus(id, 1)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));
        return mapToProductResponse(product);
    }

    @Override
    public List<ProductResponse> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return productRepository.searchByNameOrSku(keyword.trim()).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public InventorySummaryResponse getInventorySummary() {
        Integer totalStock = productVariantRepository.getTotalStockQuantity();
        if (totalStock == null) totalStock = 0;

        List<ProductVariant> lowStockVariants = productVariantRepository.findLowStockVariants(5);

        List<InventorySummaryResponse.LowStockItem> lowStockItems = lowStockVariants.stream()
                .map(v -> InventorySummaryResponse.LowStockItem.builder()
                        .productId(v.getProduct().getId())
                        .productName(v.getProduct().getName())
                        .sku(v.getProduct().getSku())
                        .variantId(v.getId())
                        .size(v.getSize())
                        .color(v.getColor())
                        .stockQuantity(v.getStockQuantity())
                        .build())
                .collect(Collectors.toList());

        return InventorySummaryResponse.builder()
                .totalStockQuantity(totalStock)
                .lowStockThreshold(5)
                .lowStockCount(lowStockItems.size())
                .lowStockItems(lowStockItems)
                .build();
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findByStatus(1).stream()
                .map(c -> CategoryDTO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .slug(c.getSlug())
                        .description(c.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    private ProductResponse mapToProductResponse(Product product) {
        List<String> imageUrls = product.getImages() != null
                ? product.getImages().stream().map(ProductImage::getImageUrl).collect(Collectors.toList())
                : new ArrayList<>();

        String thumbnailUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);

        List<ProductVariantResponse> variants = product.getVariants() != null
                ? product.getVariants().stream().map(v -> ProductVariantResponse.builder()
                .id(v.getId())
                .size(v.getSize())
                .color(v.getColor())
                .stockQuantity(v.getStockQuantity())
                .skuVariant(v.getSkuVariant())
                .build()).collect(Collectors.toList())
                : new ArrayList<>();

        int totalStock = variants.stream().mapToInt(ProductVariantResponse::getStockQuantity).sum();

        Category category = product.getCategory();
        boolean isCatActive = category != null && (category.getStatus() == null || category.getStatus() == 1);
        Long categoryId = isCatActive ? category.getId() : null;
        String categoryName = isCatActive ? category.getName() : "Chưa phân loại";

        Double avgRatingRaw = reviewRepository.getAverageRatingByProductId(product.getId());
        Long rCountRaw = reviewRepository.getReviewCountByProductId(product.getId());
        Double averageRating = avgRatingRaw != null ? Math.round(avgRatingRaw * 10.0) / 10.0 : 0.0;
        Long reviewCount = rCountRaw != null ? rCountRaw : 0L;

        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .status(product.getStatus())
                .imageUrls(imageUrls)
                .thumbnailUrl(thumbnailUrl)
                .variants(variants)
                .totalStock(totalStock)
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .createdAt(product.getCreatedAt())
                .build();
    }
}

// Feature Implementation: api update product vào db
