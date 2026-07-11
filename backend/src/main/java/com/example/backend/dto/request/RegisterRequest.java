package com.example.backend.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    private String username;
    private String password;
    private String email;
    private String fullName;
    private String phone;
    private String address;
}
