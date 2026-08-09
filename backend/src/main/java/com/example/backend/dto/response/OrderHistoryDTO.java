package com.example.backend.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistoryDTO {

    private Long id;
    private String status;
    private String description;
    private LocalDateTime createdAt;
}
