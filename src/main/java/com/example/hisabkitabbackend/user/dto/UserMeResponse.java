package com.example.hisabkitabbackend.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserMeResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
