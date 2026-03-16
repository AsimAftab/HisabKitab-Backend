package com.example.hisabkitabbackend.transaction.dto;

import java.math.BigDecimal;

public record CurrentMonthBalanceResponse(
        int year,
        int month,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal balance
) {
}
