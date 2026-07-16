package com.example.backend.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Long categoryId;
    private String categoryName;
    private Integer status;
    private List<String> imageUrls;
    private String thumbnailUrl;
    private List<ProductVariantResponse> variants;
    private Integer totalStock;
    private Double averageRating;
    private Long reviewCount;
    private LocalDateTime createdAt;
}
