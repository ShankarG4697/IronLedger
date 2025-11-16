package com.ironledger.wallet.repository;

import com.ironledger.wallet.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    @Modifying
    @Query("UPDATE AuthSession s SET s.revokedAt = CURRENT_TIMESTAMP WHERE s.userId = :userId")
    void revokeAllSessionsForUser(UUID userId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM AuthSession s WHERE s.revokedAt IS NULL AND s.userId = :userId AND s.id = :sessionId")
    boolean existsActiveSession(UUID userId, UUID sessionId);

    @Query("SELECT s FROM AuthSession s WHERE s.id = :id")
    Optional<AuthSession> findBySessionId(UUID id);
}
