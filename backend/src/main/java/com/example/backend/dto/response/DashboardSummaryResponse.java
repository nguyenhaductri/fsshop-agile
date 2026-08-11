package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private Long totalRevenue;
    private Long totalCompletedOrders;
    private Long totalProductsSold;

    private PeriodSummaryDTO summaryToday;
    private PeriodSummaryDTO summaryMonth;
    private PeriodSummaryDTO summaryYear;
    private PeriodSummaryDTO summaryAll;
    private PeriodSummaryDTO summaryCustom;

    private List<MonthlyRevenueDTO> monthlyRevenues;
    private List<TopProductDTO> topProducts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodSummaryDTO {
        private Long totalRevenue;
        private Long completedOrders;
        private Long productsSold;
        private Long cancelledOrders;
        private Long totalOrders;
        private Double cancellationRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenueDTO {
        private int month;
        private int year;
        private Long revenue;
        private Long orderCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductDTO {
        private Long productId;
        private String productName;
        private Long quantitySold;
        private Long revenue;

        @Builder.Default
        private Long quantitySoldToday = 0L;
        @Builder.Default
        private Long revenueToday = 0L;

        @Builder.Default
        private Long quantitySoldMonth = 0L;
        @Builder.Default
        private Long revenueMonth = 0L;

        @Builder.Default
        private Long quantitySoldYear = 0L;
        @Builder.Default
        private Long revenueYear = 0L;

        @Builder.Default
        private Long quantitySoldAll = 0L;
        @Builder.Default
        private Long revenueAll = 0L;

        @Builder.Default
        private Long quantitySoldCustom = 0L;
        @Builder.Default
        private Long revenueCustom = 0L;
    }
}
