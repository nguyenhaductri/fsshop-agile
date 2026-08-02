package com.example.backend.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String thumbnailUrl;
    private Long variantId;
    private String size;
    private String color;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subTotal;
}
