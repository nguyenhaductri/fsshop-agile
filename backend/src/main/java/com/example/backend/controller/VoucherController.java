package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.VoucherResponse;
import com.example.backend.dto.response.VoucherValidationResponse;
import com.example.backend.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getActiveVouchers() {
        List<VoucherResponse> list = voucherService.getActiveVouchers();
        return ResponseEntity.ok(ApiResponse.ok(list, "Lấy danh sách Voucher khả dụng thành công!"));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<VoucherValidationResponse>> validateVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal orderAmount) {
        VoucherValidationResponse response = voucherService.validateVoucher(code, orderAmount);
        return ResponseEntity.ok(ApiResponse.ok(response, response.getMessage()));
    }
}

// Feature Implementation: làm hệ thống mã giảm giá voucher và quản lý admin voucher
