package com.example.backend.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddressResponse {

    private Long id;
    private Long userId;
    private String recipientName;
    private String recipientPhone;
    private String province;
    private String district;
    private String ward;
    private String detailAddress;
    private String fullAddress;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}
