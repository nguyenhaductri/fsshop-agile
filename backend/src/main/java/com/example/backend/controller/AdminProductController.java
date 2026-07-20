package com.example.backend.controller;

import com.example.backend.dto.request.ProductRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.CategoryDTO;
import com.example.backend.dto.response.InventorySummaryResponse;
import com.example.backend.dto.response.ProductResponse;
import com.example.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Thêm sản phẩm mới thành công!"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật sản phẩm thành công!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProductSoft(@PathVariable Long id) {
        productService.deleteProductSoft(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa mềm sản phẩm thành công!"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ProductResponse> products = productService.getAllProducts(PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(ApiResponse.ok(products, "Lấy danh sách sản phẩm thành công!"));
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<InventorySummaryResponse>> getInventorySummary() {
        InventorySummaryResponse inventory = productService.getInventorySummary();
        return ResponseEntity.ok(ApiResponse.ok(inventory, "Lấy thống kê tồn kho thành công!"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getCategories() {
        List<CategoryDTO> categories = productService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.ok(categories, "Lấy danh sách danh mục thành công!"));
    }
}

// Feature Implementation: api insert product & file
