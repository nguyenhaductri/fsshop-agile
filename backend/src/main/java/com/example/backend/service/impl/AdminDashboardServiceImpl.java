package com.example.backend.service.impl;

import com.example.backend.dto.response.DashboardSummaryResponse;
import com.example.backend.entity.Product;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    public DashboardSummaryResponse getSummary(Integer year, String fromDate, String toDate) {
        int targetYear = (year != null) ? year : LocalDateTime.now().getYear();

        LocalDateTime customStart = parseStartDateTime(fromDate, targetYear);
        LocalDateTime customEnd = parseEndDateTime(toDate, targetYear);

        // 1. Overall & Period-based Summaries
        List<Object[]> allOrdersSummary = orderRepository.findAllOrdersSummary();
        List<Object[]> allCompletedItems = orderRepository.findAllCompletedOrderItems();

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        int curMonth = now.getMonthValue();
        int curYear = now.getYear();

        class PeriodCounter {
            long revenue = 0L;
            long completedOrders = 0L;
            long productsSold = 0L;
            long cancelledOrders = 0L;
            long totalOrders = 0L;

            DashboardSummaryResponse.PeriodSummaryDTO toDTO() {
                double rate = totalOrders > 0 ? (double) cancelledOrders * 100.0 / totalOrders : 0.0;
                rate = Math.round(rate * 10.0) / 10.0;
                return DashboardSummaryResponse.PeriodSummaryDTO.builder()
                        .totalRevenue(revenue)
                        .completedOrders(completedOrders)
                        .productsSold(productsSold)
                        .cancelledOrders(cancelledOrders)
                        .totalOrders(totalOrders)
                        .cancellationRate(rate)
                        .build();
            }
        }

        PeriodCounter pToday = new PeriodCounter();
        PeriodCounter pMonth = new PeriodCounter();
        PeriodCounter pYear = new PeriodCounter();
        PeriodCounter pAll = new PeriodCounter();
        PeriodCounter pCustom = new PeriodCounter();

        for (Object[] row : allOrdersSummary) {
            BigDecimal amount = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            String status = (String) row[2];
            LocalDateTime createdAt = (LocalDateTime) row[3];

            boolean isToday = createdAt != null && createdAt.toLocalDate().equals(today);
            boolean isMonth = createdAt != null && createdAt.getYear() == targetYear && createdAt.getMonthValue() == curMonth;
            boolean isYear = createdAt != null && createdAt.getYear() == targetYear;
            boolean isCustom = createdAt != null && !createdAt.isBefore(customStart) && !createdAt.isAfter(customEnd);

            pAll.totalOrders++;
            if (isToday) pToday.totalOrders++;
            if (isMonth) pMonth.totalOrders++;
            if (isYear) pYear.totalOrders++;
            if (isCustom) pCustom.totalOrders++;

            if ("COMPLETED".equalsIgnoreCase(status)) {
                long amt = amount.longValue();
                pAll.completedOrders++;
                pAll.revenue += amt;

                if (isToday) { pToday.completedOrders++; pToday.revenue += amt; }
                if (isMonth) { pMonth.completedOrders++; pMonth.revenue += amt; }
                if (isYear)  { pYear.completedOrders++;  pYear.revenue += amt; }
                if (isCustom){ pCustom.completedOrders++; pCustom.revenue += amt; }
            } else if ("CANCELLED".equalsIgnoreCase(status)) {
                pAll.cancelledOrders++;
                if (isToday) pToday.cancelledOrders++;
                if (isMonth) pMonth.cancelledOrders++;
                if (isYear)  pYear.cancelledOrders++;
                if (isCustom) pCustom.cancelledOrders++;
            }
        }

        for (Object[] row : allCompletedItems) {
            Long qty = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            LocalDateTime createdAt = (LocalDateTime) row[4];

            boolean isToday = createdAt != null && createdAt.toLocalDate().equals(today);
            boolean isMonth = createdAt != null && createdAt.getYear() == targetYear && createdAt.getMonthValue() == curMonth;
            boolean isYear = createdAt != null && createdAt.getYear() == targetYear;
            boolean isCustom = createdAt != null && !createdAt.isBefore(customStart) && !createdAt.isAfter(customEnd);

            pAll.productsSold += qty;
            if (isToday) pToday.productsSold += qty;
            if (isMonth) pMonth.productsSold += qty;
            if (isYear)  pYear.productsSold += qty;
            if (isCustom) pCustom.productsSold += qty;
        }

        // 2. Monthly revenue for 12 months starting from customStart month
        YearMonth startYM = YearMonth.of(customStart.getYear(), customStart.getMonthValue());
        Map<YearMonth, long[]> ymMap = new HashMap<>();
        for (Object[] row : allOrdersSummary) {
            BigDecimal amount = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            String status = (String) row[2];
            LocalDateTime createdAt = (LocalDateTime) row[3];
            if ("COMPLETED".equalsIgnoreCase(status) && createdAt != null) {
                YearMonth ym = YearMonth.of(createdAt.getYear(), createdAt.getMonthValue());
                long[] val = ymMap.computeIfAbsent(ym, k -> new long[2]);
                val[0] += amount.longValue();
                val[1] += 1;
            }
        }

        List<DashboardSummaryResponse.MonthlyRevenueDTO> monthlyRevenues = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth ym = startYM.plusMonths(i);
            long[] val = ymMap.get(ym);
            monthlyRevenues.add(DashboardSummaryResponse.MonthlyRevenueDTO.builder()
                    .month(ym.getMonthValue())
                    .year(ym.getYear())
                    .revenue(val != null ? val[0] : 0L)
                    .orderCount(val != null ? val[1] : 0L)
                    .build());
        }

        // 3. Detailed Product Sales with Periods (Today, Month, Year, All, Custom)
        Map<Long, DashboardSummaryResponse.TopProductDTO> salesMap = new HashMap<>();
        for (Object[] row : allCompletedItems) {
            Long pId = row[0] != null ? ((Number) row[0]).longValue() : null;
            String pName = (String) row[1];
            Long qty = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            Long rev = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            LocalDateTime createdAt = (LocalDateTime) row[4];

            if (pId == null) continue;

            DashboardSummaryResponse.TopProductDTO dto = salesMap.computeIfAbsent(pId, id ->
                DashboardSummaryResponse.TopProductDTO.builder()
                        .productId(id)
                        .productName(pName)
                        .quantitySold(0L)
                        .revenue(0L)
                        .quantitySoldToday(0L)
                        .revenueToday(0L)
                        .quantitySoldMonth(0L)
                        .revenueMonth(0L)
                        .quantitySoldYear(0L)
                        .revenueYear(0L)
                        .quantitySoldAll(0L)
                        .revenueAll(0L)
                        .quantitySoldCustom(0L)
                        .revenueCustom(0L)
                        .build()
            );

            // All time
            dto.setQuantitySoldAll(dto.getQuantitySoldAll() + qty);
            dto.setRevenueAll(dto.getRevenueAll() + rev);
            dto.setQuantitySold(dto.getQuantitySoldAll());
            dto.setRevenue(dto.getRevenueAll());

            if (createdAt != null) {
                if (createdAt.toLocalDate().equals(today)) {
                    dto.setQuantitySoldToday(dto.getQuantitySoldToday() + qty);
                    dto.setRevenueToday(dto.getRevenueToday() + rev);
                }
                if (createdAt.getYear() == targetYear && createdAt.getMonthValue() == curMonth) {
                    dto.setQuantitySoldMonth(dto.getQuantitySoldMonth() + qty);
                    dto.setRevenueMonth(dto.getRevenueMonth() + rev);
                }
                if (createdAt.getYear() == targetYear) {
                    dto.setQuantitySoldYear(dto.getQuantitySoldYear() + qty);
                    dto.setRevenueYear(dto.getRevenueYear() + rev);
                }
                if (!createdAt.isBefore(customStart) && !createdAt.isAfter(customEnd)) {
                    dto.setQuantitySoldCustom(dto.getQuantitySoldCustom() + qty);
                    dto.setRevenueCustom(dto.getRevenueCustom() + rev);
                }
            }
        }

        List<Product> allProducts = productRepository.findAll();
        List<DashboardSummaryResponse.TopProductDTO> topProducts = new ArrayList<>();
        Set<Long> processedIds = new HashSet<>();

        for (Product p : allProducts) {
            processedIds.add(p.getId());
            if (salesMap.containsKey(p.getId())) {
                topProducts.add(salesMap.get(p.getId()));
            } else {
                topProducts.add(DashboardSummaryResponse.TopProductDTO.builder()
                        .productId(p.getId())
                        .productName(p.getName())
                        .quantitySold(0L)
                        .revenue(0L)
                        .quantitySoldToday(0L)
                        .revenueToday(0L)
                        .quantitySoldMonth(0L)
                        .revenueMonth(0L)
                        .quantitySoldYear(0L)
                        .revenueYear(0L)
                        .quantitySoldAll(0L)
                        .revenueAll(0L)
                        .quantitySoldCustom(0L)
                        .revenueCustom(0L)
                        .build());
            }
        }

        for (Map.Entry<Long, DashboardSummaryResponse.TopProductDTO> entry : salesMap.entrySet()) {
            if (!processedIds.contains(entry.getKey())) {
                topProducts.add(entry.getValue());
            }
        }

        topProducts.sort((a, b) -> {
            int cmp = Long.compare(b.getQuantitySold(), a.getQuantitySold());
            if (cmp != 0) return cmp;
            return Long.compare(b.getRevenue(), a.getRevenue());
        });

        return DashboardSummaryResponse.builder()
                .totalRevenue(pAll.revenue)
                .totalCompletedOrders(pAll.completedOrders)
                .totalProductsSold(pAll.productsSold)
                .summaryToday(pToday.toDTO())
                .summaryMonth(pMonth.toDTO())
                .summaryYear(pYear.toDTO())
                .summaryAll(pAll.toDTO())
                .summaryCustom(pCustom.toDTO())
                .monthlyRevenues(monthlyRevenues)
                .topProducts(topProducts)
                .build();
    }

    private LocalDateTime parseStartDateTime(String str, int defaultYear) {
        if (str == null || str.trim().isEmpty()) {
            return LocalDateTime.of(defaultYear, 1, 1, 0, 0, 0);
        }
        try {
            str = str.trim();
            if (str.length() == 7) { // "2024-06"
                String[] parts = str.split("-");
                return LocalDateTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1, 0, 0, 0);
            }
            if (str.length() >= 10) { // "2024-06-01"
                LocalDate ld = LocalDate.parse(str.substring(0, 10));
                return ld.atStartOfDay();
            }
        } catch (Exception ignored) {}
        return LocalDateTime.of(defaultYear, 1, 1, 0, 0, 0);
    }

    private LocalDateTime parseEndDateTime(String str, int defaultYear) {
        if (str == null || str.trim().isEmpty()) {
            return LocalDateTime.of(defaultYear, 12, 31, 23, 59, 59);
        }
        try {
            str = str.trim();
            if (str.length() == 7) { // "2026-01"
                String[] parts = str.split("-");
                YearMonth ym = YearMonth.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                return ym.atEndOfMonth().atTime(23, 59, 59);
            }
            if (str.length() >= 10) { // "2026-01-31"
                LocalDate ld = LocalDate.parse(str.substring(0, 10));
                return ld.atTime(23, 59, 59);
            }
        } catch (Exception ignored) {}
        return LocalDateTime.of(defaultYear, 12, 31, 23, 59, 59);
    }
}
