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
@Table(
        name = "core_account",
        indexes = {
                @Index(name = "idx_account_user_id", columnList = "user_id"),
                @Index(name = "idx_account_currency", columnList = "currency"),
                @Index(name = "idx_account_status", columnList = "status"),
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_user_currency", columnNames = {"user_id", "currency"})
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    // ---------------------------------------------------------------------
    // STATUS CONSTANTS (clean int model)
    // ---------------------------------------------------------------------
    public static final int ACTIVE = 1;
    public static final int DISABLED = 0;
    public static final int CLOSED = -1;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    @Column(nullable = false, length = 3, updatable = false)
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

    // ---------------------------------------------------------------------
    // VALIDATION
    // ---------------------------------------------------------------------

    @PrePersist
    public void onCreate() {
        if (currency != null) {
            currency = currency.toUpperCase();
        }
        if (status == null) {
            status = ACTIVE;
        }
        if (balanceAvailable == null) balanceAvailable = 0L;
        if (balancePending == null) balancePending = 0L;
    }

    // ---------------------------------------------------------------------
    // STATUS HELPERS
    // ---------------------------------------------------------------------

    public boolean isActive() {
        return status != null && status == ACTIVE;
    }

    public boolean isDisabled() {
        return status != null && status == DISABLED;
    }

    public boolean isClosed() {
        return status != null && status == CLOSED;
    }

    // ---------------------------------------------------------------------
    // LEDGER GUARDS (NO ACTUAL BALANCE OPERATIONS IN ENTITY)
    // ---------------------------------------------------------------------

    public void assertCanDebit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }
        if (balanceAvailable < amount) {
            throw new IllegalStateException("Insufficient available balance");
        }
    }

    public void assertAmountPositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }
    }
}
