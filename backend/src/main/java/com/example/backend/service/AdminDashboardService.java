package com.example.backend.service;

import com.example.backend.dto.response.DashboardSummaryResponse;

public interface AdminDashboardService {
    DashboardSummaryResponse getSummary(Integer year, String fromDate, String toDate);
}