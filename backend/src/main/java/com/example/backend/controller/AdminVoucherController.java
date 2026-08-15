package com.example.backend.controller;

import com.example.backend.dto.request.VoucherRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.VoucherResponse;
import com.example.backend.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
public class AdminVoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getAllVouchers() {
        List<VoucherResponse> vouchers = voucherService.getAllVouchers();
        return ResponseEntity.ok(ApiResponse.ok(vouchers, "Lấy danh sách Voucher thành công!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucherById(@PathVariable Long id) {
        VoucherResponse voucher = voucherService.getVoucherById(id);
        return ResponseEntity.ok(ApiResponse.ok(voucher, "Lấy thông tin Voucher thành công!"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(@RequestBody VoucherRequest request) {
        VoucherResponse created = voucherService.createVoucher(request);
        return ResponseEntity.ok(ApiResponse.ok(created, "Tạo mới Voucher thành công!"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(@PathVariable Long id, @RequestBody VoucherRequest request) {
        VoucherResponse updated = voucherService.updateVoucher(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Cập nhật Voucher thành công!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa Voucher thành công!"));
    }
}
