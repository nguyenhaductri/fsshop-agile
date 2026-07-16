package com.example.backend.service;

import com.example.backend.dto.request.ProductRequest;
import com.example.backend.dto.response.CategoryDTO;
import com.example.backend.dto.response.InventorySummaryResponse;
import com.example.backend.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProductSoft(Long id);

    Page<ProductResponse> getAllProducts(Pageable pageable);

    Page<ProductResponse> filterProducts(
            String keyword,
            Long categoryId,
            String size,
            String color,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            Pageable pageable
    );

    ProductResponse getProductById(Long id);

    List<ProductResponse> searchProducts(String keyword);

    InventorySummaryResponse getInventorySummary();

    List<CategoryDTO> getAllCategories();
}

// Feature Implementation: api get product details by id
