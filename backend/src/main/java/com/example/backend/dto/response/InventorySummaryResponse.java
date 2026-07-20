package com.example.backend.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySummaryResponse {

    private Integer totalStockQuantity;
    private Integer lowStockThreshold;
    private Integer lowStockCount;
    private List<LowStockItem> lowStockItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LowStockItem {
        private Long productId;
        private String productName;
        private String sku;
        private Long variantId;
        private String size;
        private String color;
        private Integer stockQuantity;
    }
}
