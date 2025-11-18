package com.ironledger.wallet.repository;

import com.ironledger.wallet.entity.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerRepository extends JpaRepository<LedgerTransaction, UUID> {
    Optional<LedgerTransaction> findByAccountId(UUID accountId);
    Optional<LedgerTransaction> findByReferenceId(String referenceId);
}
