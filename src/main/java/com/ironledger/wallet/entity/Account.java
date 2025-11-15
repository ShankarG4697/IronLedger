package com.ironledger.wallet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "core_account", indexes = {
    @Index(name = "idx_account_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer status;

    @NotNull(message = "Available balance is required")
    @PositiveOrZero(message = "Available balance cannot be negative")
    @Column(name = "balance_available", nullable = false)
    @Builder.Default
    private Long balanceAvailable = 0L;

    @NotNull(message = "Pending balance is required")
    @PositiveOrZero(message = "Pending balance cannot be negative")
    @Column(name = "balance_pending", nullable = false)
    @Builder.Default
    private Long balancePending = 0L;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Custom setter for currency to ensure uppercase
     */
    public void setCurrency(String currency) {
        this.currency = currency != null ? currency.toUpperCase() : null;
    }

    /**
     * Debit available balance
     */
    public void debitAvailable(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (this.balanceAvailable < amount) {
            throw new IllegalStateException("Insufficient available balance");
        }
        this.balanceAvailable -= amount;
    }

    /**
     * Credit available balance
     */
    public void creditAvailable(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.balanceAvailable += amount;
    }

    /**
     * Move amount from pending to available
     */
    public void settlePending(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (this.balancePending < amount) {
            throw new IllegalStateException("Insufficient pending balance");
        }
        this.balancePending -= amount;
        this.balanceAvailable += amount;
    }
}
