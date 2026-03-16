package com.example.hisabkitabbackend.transaction;

import com.example.hisabkitabbackend.common.response.ApiResponse;
import com.example.hisabkitabbackend.common.response.PagedResponse;
import com.example.hisabkitabbackend.transaction.dto.CurrentMonthBalanceResponse;
import com.example.hisabkitabbackend.transaction.dto.CreateTransactionRequest;
import com.example.hisabkitabbackend.transaction.dto.TransactionResponse;
import com.example.hisabkitabbackend.transaction.dto.UpdateTransactionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Create a transaction entry")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Transaction created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List current user's transactions")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<TransactionResponse> response = transactionService.getTransactions(
                userDetails.getUsername(),
                type,
                from,
                to,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Transactions fetched successfully", response));
    }

    @GetMapping("/summary/current-month")
    @Operation(summary = "Get current month's balance summary")
    public ResponseEntity<ApiResponse<CurrentMonthBalanceResponse>> getCurrentMonthBalance(
            @AuthenticationPrincipal UserDetails userDetails) {
        CurrentMonthBalanceResponse response = transactionService.getCurrentMonthBalance(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Current month balance fetched successfully", response));
    }

    @PutMapping("/{transactionId}")
    @Operation(summary = "Update a transaction entry")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID transactionId,
            @Valid @RequestBody UpdateTransactionRequest request) {
        TransactionResponse response = transactionService.updateTransaction(
                userDetails.getUsername(),
                transactionId,
                request
        );
        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully", response));
    }

    @DeleteMapping("/{transactionId}")
    @Operation(summary = "Delete a transaction entry")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID transactionId) {
        transactionService.deleteTransaction(userDetails.getUsername(), transactionId);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully", null));
    }
}
