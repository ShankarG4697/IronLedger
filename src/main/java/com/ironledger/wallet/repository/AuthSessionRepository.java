package com.ironledger.wallet.repository;

import com.ironledger.wallet.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    @Modifying
    @Query("UPDATE AuthSession s SET s.revokedAt = CURRENT_TIMESTAMP WHERE s.userId = :userId")
    void revokeAllSessionsForUser(UUID userId);
}
