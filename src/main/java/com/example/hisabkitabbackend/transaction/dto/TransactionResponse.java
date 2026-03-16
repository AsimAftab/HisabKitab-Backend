package com.example.hisabkitabbackend.transaction.dto;

import com.example.hisabkitabbackend.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        String name,
        String description,
        BigDecimal amount,
        String category,
        LocalDate date,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
