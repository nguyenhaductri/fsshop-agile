package com.example.backend.repository;

import com.example.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatus(Integer status, Pageable pageable);

    Optional<Product> findByIdAndStatus(Long id, Integer status);

    boolean existsBySku(String sku);

    @Query("SELECT p FROM Product p " +
           "WHERE p.status = 1 " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR COALESCE(p.salePrice, p.price) >= :minPrice) " +
           "AND (:maxPrice IS NULL OR COALESCE(p.salePrice, p.price) <= :maxPrice) " +
           "AND ((:size IS NULL OR :size = '') AND (:color IS NULL OR :color = '') " +
           "     OR EXISTS (SELECT v FROM ProductVariant v WHERE v.product = p " +
           "                AND (:size IS NULL OR :size = '' OR LOWER(v.size) = LOWER(:size)) " +
           "                AND (:color IS NULL OR :color = '' OR LOWER(v.color) LIKE LOWER(CONCAT('%', :color, '%')))))")
    Page<Product> filterProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("size") String size,
            @Param("color") String color,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE p.status = 1 AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchByNameOrSku(@Param("keyword") String keyword);
}

// Feature Implementation: api get products (pagination)

// Feature Implementation: api tìm kiếm theo tên
