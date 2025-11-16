package com.ironledger.wallet.service;

import com.ironledger.wallet.dto.AccountCreateRequest;
import com.ironledger.wallet.dto.AccountResponse;
import com.ironledger.wallet.entity.Account;
import com.ironledger.wallet.exception.DuplicateResourceException;
import com.ironledger.wallet.exception.ResourceNotFoundException;
import com.ironledger.wallet.repository.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    // -------------------------------------------------------------------------
    // CREATE ACCOUNT
    // -------------------------------------------------------------------------
    @Transactional
    public AccountResponse createAccount(UUID userId, AccountCreateRequest accountCreateRequest) {
        if (accountRepository.existsByUserIdAndCurrency(userId, accountCreateRequest.getCurrency())) {
            throw new DuplicateResourceException("Account already exists for this currency");
        }

        Account account = Account.builder()
                .userId(userId)
                .currency(accountCreateRequest.getCurrency().toUpperCase())
                .status(Account.ACTIVE)
                .balanceAvailable(0L)
                .balancePending(0L)
                .build();

        accountRepository.save(account);

        return AccountResponse.from(account);
    }

    // -------------------------------------------------------------------------
    // GET ALL ACCOUNTS FOR USER
    // -------------------------------------------------------------------------
    public List<AccountResponse> listAccounts(UUID userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // GET SINGLE ACCOUNT
    // -------------------------------------------------------------------------
    public AccountResponse getAccount(UUID userId, UUID accountId) {
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!acc.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Account not found");
        }

        return AccountResponse.from(acc);
    }

    // -------------------------------------------------------------------------
    // INTERNAL API — PESSIMISTIC LOCKING FOR BALANCE OPERATIONS
    // -------------------------------------------------------------------------
    @Transactional
    public Account loadAccountForUpdate(UUID userId, UUID accountId) {
        Account acc = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!acc.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Account not found");
        }

        if (!acc.isActive()) {
            throw new IllegalStateException("Account inactive");
        }

        return acc;
    }

    // -------------------------------------------------------------------------
    // INTERNAL ONLY — UPDATE BALANCES SAFELY
    // (Actual debit/credit logic belongs inside TransactionService)
    // -------------------------------------------------------------------------
    @Transactional
    public void updateAvailableBalance(UUID userId, UUID accountId, long delta) {
        if (delta == 0) return;

        Account acc = loadAccountForUpdate(userId, accountId);

        if (delta > 0) {
            acc.setBalanceAvailable(acc.getBalanceAvailable() + delta);
        } else {
            long withdraw = -delta;
            acc.assertCanDebit(withdraw);
            acc.setBalanceAvailable(acc.getBalanceAvailable() - withdraw);
        }

        accountRepository.save(acc);
    }

    @Transactional
    public void updatePendingBalance(UUID userId, UUID accountId, long delta) {
        Account acc = loadAccountForUpdate(userId, accountId);

        long newPending = acc.getBalancePending() + delta;
        if (newPending < 0) throw new IllegalStateException("Negative pending balance");

        acc.setBalancePending(newPending);

        accountRepository.save(acc);
    }
}
