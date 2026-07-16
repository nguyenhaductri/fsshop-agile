package com.example.backend.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantRequest {

    private Long id;
    private String size;
    private String color;
    private Integer stockQuantity;
    private String skuVariant;
}
