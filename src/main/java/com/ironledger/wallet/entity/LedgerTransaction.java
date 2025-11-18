package com.ironledger.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "core_ledger_transaction",
        indexes = {
                @Index(name = "idx_ledger_account_id", columnList = "account_id"),
                @Index(name = "idx_ledger_user_id", columnList = "user_id"),
                @Index(name = "idx_ledger_reference_id", columnList = "reference_id"),
                @Index(name = "idx_ledger_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Long amount;  // +credit, -debit

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String type; // PENDING_DEBIT, CAPTURE, CREDIT, etc.

    @Column(nullable = false)
    private Integer status; // 1=confirmed, 0=pending, -1=failed

    @Column(name = "balance_before", nullable = false)
    private Long balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "pending_before", nullable = false)
    private Long pendingBefore;

    @Column(name = "pending_after", nullable = false)
    private Long pendingAfter;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "external_ref")
    private String externalRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> meta;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
