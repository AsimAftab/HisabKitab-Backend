package com.example.hisabkitabbackend.transaction;

import com.example.hisabkitabbackend.auth.exception.AuthException;
import com.example.hisabkitabbackend.common.exception.BusinessException;
import com.example.hisabkitabbackend.common.response.PagedResponse;
import com.example.hisabkitabbackend.transaction.dto.CurrentMonthBalanceResponse;
import com.example.hisabkitabbackend.transaction.dto.CreateTransactionRequest;
import com.example.hisabkitabbackend.transaction.dto.TransactionResponse;
import com.example.hisabkitabbackend.transaction.dto.UpdateTransactionRequest;
import com.example.hisabkitabbackend.user.User;
import com.example.hisabkitabbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public TransactionResponse createTransaction(String email, CreateTransactionRequest request) {
        User user = getUserByEmail(email);

        Transaction transaction = Transaction.builder()
                .user(user)
                .type(request.type())
                .name(request.name().trim())
                .description(trimToNull(request.description()))
                .amount(request.amount())
                .category(request.category().trim())
                .date(request.date())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Created transaction {} for user {} with type {}", saved.getId(), email, saved.getType());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactions(
            String email,
            TransactionType type,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {
        User user = getUserByEmail(email);
        validateDateRange(from, to);
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("date"),
                Sort.Order.desc("createdAt")
        ));
        Page<Transaction> transactions;

        if (from != null && to != null) {
            transactions = type == null
                    ? transactionRepository.findAllByUserIdAndDateBetween(user.getId(), from, to, pageable)
                    : transactionRepository.findAllByUserIdAndTypeAndDateBetween(user.getId(), type, from, to, pageable);
        } else {
            transactions = type == null
                    ? transactionRepository.findAllByUserId(user.getId(), pageable)
                    : transactionRepository.findAllByUserIdAndType(user.getId(), type, pageable);
        }

        log.info(
                "Fetched {} transactions for user {} with type filter {}, from {}, to {}, page {}, size {}",
                transactions.getNumberOfElements(),
                email,
                type,
                from,
                to,
                page,
                size
        );

        return new PagedResponse<>(
                transactions.getContent().stream().map(this::toResponse).toList(),
                transactions.getNumber(),
                transactions.getSize(),
                transactions.getTotalElements(),
                transactions.getTotalPages(),
                transactions.hasNext(),
                transactions.hasPrevious()
        );
    }

    @Transactional(readOnly = true)
    public CurrentMonthBalanceResponse getCurrentMonthBalance(String email) {
        User user = getUserByEmail(email);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.withDayOfMonth(1);
        LocalDate endDate = today.withDayOfMonth(today.lengthOfMonth());

        MonthlyBalanceSummary summary = transactionRepository.getMonthlyBalanceSummary(user.getId(), startDate, endDate);
        BigDecimal incomeTotal = summary != null && summary.getIncomeTotal() != null
                ? summary.getIncomeTotal()
                : BigDecimal.ZERO;
        BigDecimal expenseTotal = summary != null && summary.getExpenseTotal() != null
                ? summary.getExpenseTotal()
                : BigDecimal.ZERO;
        BigDecimal balance = incomeTotal.subtract(expenseTotal);

        log.info(
                "Fetched current month balance for user {}: income={}, expense={}, balance={}",
                email,
                incomeTotal,
                expenseTotal,
                balance
        );

        return new CurrentMonthBalanceResponse(
                today.getYear(),
                today.getMonthValue(),
                incomeTotal,
                expenseTotal,
                balance
        );
    }

    @Transactional
    public TransactionResponse updateTransaction(String email, UUID transactionId, UpdateTransactionRequest request) {
        User user = getUserByEmail(email);
        Transaction transaction = getTransactionByIdAndUserId(transactionId, user.getId());

        transaction.setType(request.type());
        transaction.setName(request.name().trim());
        transaction.setDescription(trimToNull(request.description()));
        transaction.setAmount(request.amount());
        transaction.setCategory(request.category().trim());
        transaction.setDate(request.date());

        Transaction updated = transactionRepository.save(transaction);
        log.info("Updated transaction {} for user {}", updated.getId(), email);
        return toResponse(updated);
    }

    @Transactional
    public void deleteTransaction(String email, UUID transactionId) {
        User user = getUserByEmail(email);
        Transaction transaction = getTransactionByIdAndUserId(transactionId, user.getId());
        transactionRepository.delete(transaction);
        log.info("Deleted transaction {} for user {}", transactionId, email);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found"));
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if ((from == null) != (to == null)) {
            throw new BusinessException("Both from and to dates are required together");
        }
        if (from != null && from.isAfter(to)) {
            throw new BusinessException("'from' date cannot be after 'to' date");
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BusinessException("Page must be greater than or equal to 0");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException("Size must be between 1 and 100");
        }
    }

    private Transaction getTransactionByIdAndUserId(UUID transactionId, UUID userId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new BusinessException("Transaction not found"));
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getName(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getDate(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
