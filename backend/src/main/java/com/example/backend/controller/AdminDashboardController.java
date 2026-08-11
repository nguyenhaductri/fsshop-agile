package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.DashboardSummaryResponse;
import com.example.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        DashboardSummaryResponse summary = adminDashboardService.getSummary(year, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.ok(summary, "Thống kê doanh thu thành công!"));
    }
}

// Feature Implementation: làm trang thống kê doanh thu và báo cáo admin
