package com.ironledger.wallet.service;

import com.ironledger.wallet.context.RequestContextHolder;
import com.ironledger.wallet.dto.AmountRequest;
import com.ironledger.wallet.dto.LedgerResponse;
import com.ironledger.wallet.entity.Account;
import com.ironledger.wallet.entity.LedgerTransaction;
import com.ironledger.wallet.exception.InvalidRequestException;
import com.ironledger.wallet.exception.ResourceNotFoundException;
import com.ironledger.wallet.repository.AccountRepository;
import com.ironledger.wallet.repository.LedgerRepository;
import com.ironledger.wallet.utils.MetaBuilder;
import com.ironledger.wallet.utils.RefIdGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final LedgerRepository ledgerRepository;
    private final AccountRepository accountRepository;

    private static final ZoneId SG_ZONE = ZoneId.of("Asia/Singapore");

    // ------------------------------
    // CREDIT
    // ------------------------------
    @Transactional
    public LedgerResponse credit(UUID userId, UUID accountId, AmountRequest creditRequest) {
        if(creditRequest.getAmount() <= 0) throw new InvalidRequestException("Amount must be positive");

        String referenceId = generateReferenceId();

        //Idempotency
        Optional<LedgerTransaction> existing = findByReference(referenceId);
        if (existing.isPresent()) return (new LedgerResponse(
                existing.get().getId(), existing.get().getReferenceId(), existing.get().getType(),
                existing.get().getAmount(), existing.get().getCreatedAt()
        ));

        //Lock account
        Account account = lockAccount(userId, accountId);

        if (!account.getCurrency().equals(creditRequest.getCurrency())) {
            throw new InvalidRequestException("Currency mismatch");
        }
        long amount = creditRequest.getAmount();
        long before = account.getBalanceAvailable();

        account.setBalanceAvailable(before + amount);
        accountRepository.save(account);

        LedgerTransaction tx = writeLedger(account, amount, "CREDIT", 1,
                referenceId, account.getCurrency());

        tx.setBalanceBefore(before);
        tx.setBalanceAfter(account.getBalanceAvailable());

        ledgerRepository.save(tx);

        return (new LedgerResponse(
                tx.getId(), tx.getReferenceId(), tx.getType(), tx.getAmount(), tx.getCreatedAt()
        ));
    }

    // -------------------------------------------------------------------------
    // DIRECT DEBIT (available → decrease)
    // -------------------------------------------------------------------------
    @Transactional
    public LedgerResponse debit(UUID userId, UUID accountId, AmountRequest debitRequest) {
        if(debitRequest.getAmount() <= 0) throw new InvalidRequestException("Amount must be positive");
        String referenceId = generateReferenceId();

        //Idempotency
        Optional<LedgerTransaction> existing = findByReference(referenceId);
        if (existing.isPresent()) return (new LedgerResponse(
                existing.get().getId(), existing.get().getReferenceId(), existing.get().getType(),
                existing.get().getAmount(), existing.get().getCreatedAt()
        ));

        //Lock account
        Account account = lockAccount(userId, accountId);
        if (!account.getCurrency().equals(debitRequest.getCurrency())) {
            throw new InvalidRequestException("Currency mismatch");
        }
        long amount = debitRequest.getAmount();

        if (account.getBalanceAvailable() < amount) {
            throw new InvalidRequestException("Insufficient funds");
        }

        long before = account.getBalanceAvailable();

        account.setBalanceAvailable(before - amount);
        accountRepository.save(account);

        LedgerTransaction tx = writeLedger(account, -amount, "DEBIT", 1,
                referenceId, account.getCurrency());

        tx.setBalanceBefore(before);
        tx.setBalanceAfter(account.getBalanceAvailable());

        ledgerRepository.save(tx);

        return (new LedgerResponse(
                tx.getId(), tx.getReferenceId(), tx.getType(), tx.getAmount(), tx.getCreatedAt()
        ));
    }

    // -------------------------------------------------------------------------
    // PENDING DEBIT (authorization hold)
    // -------------------------------------------------------------------------
    @Transactional
    public LedgerResponse pendingDebit(UUID userId, UUID accountId, AmountRequest debitRequest) {
        if(debitRequest.getAmount() <= 0) throw new InvalidRequestException("Amount must be positive");
        String referenceId = generateReferenceId();

        //Idempotency
        Optional<LedgerTransaction> existing = findByReference(referenceId);
        if (existing.isPresent()) return (new LedgerResponse(
                existing.get().getId(), existing.get().getReferenceId(), existing.get().getType(),
                existing.get().getAmount(), existing.get().getCreatedAt()
        ));

        //Lock account
        Account account = lockAccount(userId, accountId);
        if (!account.getCurrency().equals(debitRequest.getCurrency())) {
            throw new InvalidRequestException("Currency mismatch");
        }
        long amount = debitRequest.getAmount();

        if (account.getBalanceAvailable() < amount) {
            throw new InvalidRequestException("Insufficient funds");
        }

        long beforePending = account.getBalancePending();
        long beforeAvail = account.getBalanceAvailable();

        // Move funds available → pending
        account.setBalanceAvailable(beforeAvail - amount);
        account.setBalancePending(beforePending + amount);
        accountRepository.save(account);

        LedgerTransaction tx = writeLedger(account, -amount, "PENDING_DEBIT", 0,
                referenceId, account.getCurrency());

        tx.setBalanceBefore(beforeAvail);
        tx.setBalanceAfter(account.getBalanceAvailable());
        tx.setPendingBefore(beforePending);
        tx.setPendingAfter(account.getBalancePending());

        ledgerRepository.save(tx);

        return (new LedgerResponse(
                tx.getId(), tx.getReferenceId(), tx.getType(), tx.getAmount(), tx.getCreatedAt()
        ));
    }

    // -------------------------------------------------------------------------
    // CAPTURE (pending → available decrease)
    // -------------------------------------------------------------------------
    @Transactional
    public LedgerResponse capture(UUID userId, UUID accountId, AmountRequest captureRequest) {
        if(captureRequest.getAmount() <= 0) throw new InvalidRequestException("Amount must be positive");
        String referenceId = generateReferenceId();

        //Idempotency
        Optional<LedgerTransaction> existing = findByReference(referenceId);
        if (existing.isPresent()) return (new LedgerResponse(
                existing.get().getId(), existing.get().getReferenceId(), existing.get().getType(),
                existing.get().getAmount(), existing.get().getCreatedAt()
        ));

        //Lock account
        Account account = lockAccount(userId, accountId);
        if (!account.getCurrency().equals(captureRequest.getCurrency())) {
            throw new InvalidRequestException("Currency mismatch");
        }
        long amount = captureRequest.getAmount();

        if (account.getBalanceAvailable() < amount) {
            throw new InvalidRequestException("Insufficient funds");
        }

        long pendingBefore = account.getBalancePending();

        account.setBalancePending(pendingBefore - amount);
        accountRepository.save(account);

        LedgerTransaction tx = writeLedger(account, -amount, "CAPTURE", 1,
                referenceId, account.getCurrency());

        tx.setPendingBefore(pendingBefore);
        tx.setPendingAfter(account.getBalancePending());

        ledgerRepository.save(tx);

        return (new LedgerResponse(
                tx.getId(), tx.getReferenceId(), tx.getType(), tx.getAmount(), tx.getCreatedAt()
        ));
    }

    // -------------------------------------------------------------------------
    // RELEASE (void pending debit)
    // -------------------------------------------------------------------------
    @Transactional
    public LedgerResponse release(UUID userId, UUID accountId, AmountRequest releaseRequest) {
        if(releaseRequest.getAmount() <= 0) throw new InvalidRequestException("Amount must be positive");
        String referenceId = generateReferenceId();

        Optional<LedgerTransaction> existing = findByReference(referenceId);
        if (existing.isPresent()) return (new LedgerResponse(
                existing.get().getId(), existing.get().getReferenceId(), existing.get().getType(),
                existing.get().getAmount(), existing.get().getCreatedAt()
        ));

        Account account = lockAccount(userId, accountId);
        if (!account.getCurrency().equals(releaseRequest.getCurrency())) {
            throw new InvalidRequestException("Currency mismatch");
        }
        long amount = releaseRequest.getAmount();

        if (account.getBalancePending() < amount) {
            throw new InvalidRequestException("Insufficient pending funds");
        }

        long pendingBefore = account.getBalancePending();
        long availBefore = account.getBalanceAvailable();

        // Move pending → available
        account.setBalancePending(pendingBefore - amount);
        account.setBalanceAvailable(availBefore + amount);
        accountRepository.save(account);

        LedgerTransaction tx = writeLedger(account, amount, "RELEASE", 1,
                referenceId, account.getCurrency());

        tx.setPendingBefore(pendingBefore);
        tx.setPendingAfter(account.getBalancePending());
        tx.setBalanceBefore(availBefore);
        tx.setBalanceAfter(account.getBalanceAvailable());

        ledgerRepository.save(tx);

        return (new LedgerResponse(
                tx.getId(), tx.getReferenceId(), tx.getType(), tx.getAmount(), tx.getCreatedAt()
        ));
    }

    // -------------------------------------------------------------------------
    // IDEMPOTENCY CHECK
    // -------------------------------------------------------------------------
    private Optional<LedgerTransaction> findByReference(String referenceId) {
        if (referenceId == null) return Optional.empty();
        return ledgerRepository.findByReferenceId(referenceId);
    }

    // -------------------------------------------------------------------------
    // CORE: Pessimistic Lock Account
    // -------------------------------------------------------------------------
    private Account lockAccount(UUID userId, UUID accountId) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Account not found");
        }

        if (!account.isActive()) {
            throw new InvalidRequestException("Account is not active");
        }

        return account;
    }

    // -------------------------------------------------------------------------
    // WRITE LEDGER
    // -------------------------------------------------------------------------
    private LedgerTransaction writeLedger(Account account, long amount, String type, int status, String referenceId, String currency) {
        Map<String, Object> meta = MetaBuilder.buildBaseMeta(
                type,
                RequestContextHolder.getIp(),
                RequestContextHolder.getUserAgent()
        );
        meta.put("reference_id", referenceId);

        LedgerTransaction ledgerTransaction = LedgerTransaction.builder()
                .accountId(account.getId())
                .userId(account.getUserId())
                .amount(amount)
                .type(type)
                .status(status)
                .referenceId(referenceId)
                .balanceBefore(account.getBalanceAvailable())
                .balanceAfter(account.getBalanceAvailable())
                .pendingBefore(account.getBalancePending())
                .pendingAfter(account.getBalancePending())
                .createdAt(OffsetDateTime.now(SG_ZONE))
                .meta(meta)
                .currency(currency)
                .build();

        return ledgerRepository.save(ledgerTransaction);
    }

    // -------------------------------------------------------------------------
    // Reference ID Generation
    // -------------------------------------------------------------------------
    private String generateReferenceId() {
        for (int i = 0; i < 5; i++) {
            String refId = RefIdGenerator.generateRefId();
            if (ledgerRepository.findByReferenceId(refId).isEmpty()) {
                return refId;
            }
        }

        throw new IllegalStateException("Unable to generate reference ID");
    }
}
