package com.example.hisabkitabbackend.transaction;

import java.math.BigDecimal;

public interface MonthlyBalanceSummary {

    BigDecimal getIncomeTotal();

    BigDecimal getExpenseTotal();
}
