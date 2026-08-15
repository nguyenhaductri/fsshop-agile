package com.example.backend.service;

import com.example.backend.dto.request.VoucherRequest;
import com.example.backend.dto.response.VoucherResponse;
import com.example.backend.dto.response.VoucherValidationResponse;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {

    List<VoucherResponse> getAllVouchers();

    List<VoucherResponse> getActiveVouchers();

    VoucherResponse getVoucherById(Long id);

    VoucherResponse createVoucher(VoucherRequest request);

    VoucherResponse updateVoucher(Long id, VoucherRequest request);

    void deleteVoucher(Long id);

    VoucherValidationResponse validateVoucher(String code, BigDecimal orderAmount);
}
