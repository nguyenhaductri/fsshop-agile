package com.example.backend.repository;

import com.example.backend.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    @Query("SELECT pv FROM ProductVariant pv JOIN FETCH pv.product p WHERE p.status = 1 AND pv.stockQuantity < :threshold")
    List<ProductVariant> findLowStockVariants(@Param("threshold") Integer threshold);

    @Query("SELECT SUM(pv.stockQuantity) FROM ProductVariant pv JOIN pv.product p WHERE p.status = 1")
    Integer getTotalStockQuantity();
}
