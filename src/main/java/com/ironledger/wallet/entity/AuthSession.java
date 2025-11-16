package com.ironledger.wallet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "auth_session", indexes = {
        @Index(name = "idx_auth_session_user_id", columnList = "user_id"),
        @Index(name = "idx_auth_session_expires", columnList = "expires_at"),
        @Index(name = "idx_auth_session_revoked", columnList = "revoked_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
public class AuthSession {

    private static final ZoneId SG_ZONE = ZoneId.of("Asia/Singapore");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "refresh_token_hash", nullable = false, columnDefinition = "TEXT")
    private String refreshTokenHash;  // Note: Stores SHA-256 hash, not BCrypt

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address")
    private String ipAddress;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    // ------------------------------------------------------------------------
    // Lifecycle callbacks
    // ------------------------------------------------------------------------
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(SG_ZONE);
        }
        if (expiresAt == null) {
            expiresAt = OffsetDateTime.now(SG_ZONE).plusDays(14);
        }
    }

    // ------------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------------
    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(OffsetDateTime.now(SG_ZONE));
    }
}
