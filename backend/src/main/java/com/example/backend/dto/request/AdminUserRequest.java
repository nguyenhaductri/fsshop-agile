package com.example.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserRequest {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String role; // 'ROLE_USER', 'ROLE_ADMIN', 'ROLE_OWNER'
    private Integer status; // 1 = Active, 0 = Locked
}
