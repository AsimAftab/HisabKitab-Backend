package com.example.hisabkitabbackend.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findAllByUserId(UUID userId, Pageable pageable);

    Page<Transaction> findAllByUserIdAndType(UUID userId, TransactionType type, Pageable pageable);

    Page<Transaction> findAllByUserIdAndDateBetween(
            UUID userId,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    Page<Transaction> findAllByUserIdAndTypeAndDateBetween(
            UUID userId,
            TransactionType type,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select
                coalesce(sum(case when t.type = com.example.hisabkitabbackend.transaction.TransactionType.INCOME then t.amount else 0 end), 0) as incomeTotal,
                coalesce(sum(case when t.type = com.example.hisabkitabbackend.transaction.TransactionType.EXPENSE then t.amount else 0 end), 0) as expenseTotal
            from Transaction t
            where t.user.id = :userId
              and t.date between :startDate and :endDate
            """)
    MonthlyBalanceSummary getMonthlyBalanceSummary(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
