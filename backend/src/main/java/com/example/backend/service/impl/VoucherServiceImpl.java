package com.example.backend.service.impl;

import com.example.backend.dto.request.VoucherRequest;
import com.example.backend.dto.response.VoucherResponse;
import com.example.backend.dto.response.VoucherValidationResponse;
import com.example.backend.entity.Voucher;
import com.example.backend.repository.VoucherRepository;
import com.example.backend.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.backend.service.NotificationService;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final NotificationService notificationService;

    @Override
    public List<VoucherResponse> getAllVouchers() {
        return voucherRepository.findAllByOrderByIdDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoucherResponse> getActiveVouchers() {
        LocalDateTime now = LocalDateTime.now();
        return voucherRepository.findByStatusOrderByIdDesc(1).stream()
                .filter(v -> (v.getIsPublic() == null || Boolean.TRUE.equals(v.getIsPublic())) &&
                             (v.getStartDate() == null || !now.isBefore(v.getStartDate())) &&
                             (v.getEndDate() == null || !now.isAfter(v.getEndDate())) &&
                             (v.getUsageLimit() == null || v.getUsedCount() < v.getUsageLimit()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public VoucherResponse getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Voucher id: " + id));
        return mapToResponse(voucher);
    }

    @Override
    @Transactional
    public VoucherResponse createVoucher(VoucherRequest request) {
        String cleanCode = request.getCode().trim().toUpperCase();
        if (voucherRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new RuntimeException("Mã Voucher '" + cleanCode + "' đã tồn tại!");
        }

        Long targetId;
        if (request.getId() != null && request.getId() > 0) {
            if (voucherRepository.existsById(request.getId())) {
                throw new RuntimeException("Mã ID #" + request.getId() + " đã tồn tại trong hệ thống!");
            }
            targetId = request.getId();
        } else {
            targetId = voucherRepository.findMaxId() + 1;
        }

        Voucher voucher = Voucher.builder()
                .id(targetId)
                .code(cleanCode)
                .name(request.getName())
                .description(request.getDescription())
                .discountType(request.getDiscountType() != null ? request.getDiscountType().toUpperCase() : "PERCENT")
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO)
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit() != null ? request.getUsageLimit() : 100)
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .build();

        Voucher saved = voucherRepository.save(voucher);

        if (Boolean.TRUE.equals(saved.getIsPublic()) && Integer.valueOf(1).equals(saved.getStatus())) {
            notificationService.createNotification(
                    null,
                    "🎉 Voucher Mới Được Phát Hành!",
                    "FS Shop vừa phát hành Voucher công khai '" + saved.getCode() + "': " + saved.getName() + ". Hãy áp dụng ngay khi đặt hàng!",
                    "VOUCHER_NEW",
                    "checkout"
            );
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public VoucherResponse updateVoucher(Long id, VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Voucher id: " + id));

        String cleanCode = request.getCode().trim().toUpperCase();
        if (!voucher.getCode().equalsIgnoreCase(cleanCode) && voucherRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new RuntimeException("Mã Voucher '" + cleanCode + "' đã tồn tại!");
        }

        voucher.setCode(cleanCode);
        voucher.setName(request.getName());
        voucher.setDescription(request.getDescription());
        if (request.getDiscountType() != null) voucher.setDiscountType(request.getDiscountType().toUpperCase());
        voucher.setDiscountValue(request.getDiscountValue());
        if (request.getMinOrderAmount() != null) voucher.setMinOrderAmount(request.getMinOrderAmount());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        if (request.getUsageLimit() != null) voucher.setUsageLimit(request.getUsageLimit());
        if (request.getStartDate() != null) voucher.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) voucher.setEndDate(request.getEndDate());
        if (request.getIsPublic() != null) voucher.setIsPublic(request.getIsPublic());
        if (request.getStatus() != null) voucher.setStatus(request.getStatus());

        Voucher saved = voucherRepository.save(voucher);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Voucher id: " + id));
        voucherRepository.delete(voucher);
    }

    @Override
    public VoucherValidationResponse validateVoucher(String code, BigDecimal orderAmount) {
        if (code == null || code.trim().isEmpty()) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .message("Mã voucher không được để trống!")
                    .build();
        }

        String cleanCode = code.trim().toUpperCase();
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(cleanCode).orElse(null);

        if (voucher == null) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .message("Mã giảm giá '" + cleanCode + "' không tồn tại!")
                    .build();
        }

        if (voucher.getStatus() == 0) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .message("Mã giảm giá '" + cleanCode + "' đã bị khóa!")
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .message("Mã giảm giá chưa đến thời gian áp dụng!")
                    .build();
        }

        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .message("Mã giảm giá đã hết hạn sử dụng!")
                    .build();
        }

        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .message("Mã giảm giá đã hết lượt sử dụng!")
                    .build();
        }

        BigDecimal minAmount = voucher.getMinOrderAmount() != null ? voucher.getMinOrderAmount() : BigDecimal.ZERO;
        if (orderAmount != null && orderAmount.compareTo(minAmount) < 0) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .message("Đơn hàng tối thiểu để dùng mã là " + minAmount.longValue() + " ₫!")
                    .build();
        }

        // Calculate discount
        BigDecimal discount = BigDecimal.ZERO;
        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            BigDecimal percent = voucher.getDiscountValue().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            discount = orderAmount.multiply(percent).setScale(0, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(voucher.getMaxDiscountAmount());
            }
        } else { // "FIXED"
            discount = voucher.getDiscountValue();
        }

        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        BigDecimal finalAmount = orderAmount.subtract(discount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        return VoucherValidationResponse.builder()
                .valid(true)
                .code(voucher.getCode())
                .name(voucher.getName())
                .message("Áp dụng mã giảm giá thành công!")
                .discountAmount(discount)
                .originalAmount(orderAmount)
                .finalAmount(finalAmount)
                .build();
    }

    private VoucherResponse mapToResponse(Voucher v) {
        return VoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .name(v.getName())
                .description(v.getDescription())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .minOrderAmount(v.getMinOrderAmount())
                .maxDiscountAmount(v.getMaxDiscountAmount())
                .usageLimit(v.getUsageLimit())
                .usedCount(v.getUsedCount())
                .startDate(v.getStartDate())
                .endDate(v.getEndDate())
                .isPublic(v.getIsPublic() != null ? v.getIsPublic() : true)
                .status(v.getStatus())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
