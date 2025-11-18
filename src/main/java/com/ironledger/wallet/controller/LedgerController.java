package com.ironledger.wallet.controller;

import com.ironledger.wallet.dto.AmountRequest;
import com.ironledger.wallet.dto.LedgerResponse;
import com.ironledger.wallet.service.LedgerService;
import com.ironledger.wallet.utils.AuthenticationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class LedgerController {
    private final LedgerService ledgerService;

    private UUID userId(Authentication authentication) {
        return AuthenticationUtils.resolveUserIdFromAuthentication(authentication);
    }

    // ----------------------------------------
    // CREDIT
    // ----------------------------------------
    @PostMapping("/credit/{accountId}")
    public ResponseEntity<LedgerResponse> creditAccount(
            Authentication auth,
            @PathVariable UUID accountId,
            @Valid @RequestBody AmountRequest creditRequest
            ) {
        LedgerResponse response = ledgerService.credit(userId(auth), accountId, creditRequest);
        return ResponseEntity.ok(response);
    }

    // ----------------------------------------
    // DEBIT
    // ----------------------------------------
    @PostMapping("/debit/{accountId}")
    public ResponseEntity<LedgerResponse> debitAccount(
            Authentication auth,
            @PathVariable UUID accountId,
            @Valid @RequestBody AmountRequest debitRequest
            ) {
        LedgerResponse response = ledgerService.debit(userId(auth), accountId, debitRequest);
        return ResponseEntity.ok(response);
    }

    // ----------------------------------------
    // PENDING DEBIT (AUTH)
    // ----------------------------------------
    @PostMapping("/pending-debit/{accountId}")
    public ResponseEntity<LedgerResponse> pendingDebitAccount(
            Authentication auth,
            @PathVariable UUID accountId,
            @Valid @RequestBody AmountRequest debitRequest
    ) {
        LedgerResponse response = ledgerService.pendingDebit(userId(auth), accountId, debitRequest);
        return ResponseEntity.ok(response);
    }

    // ----------------------------------------
    // CAPTURE (SETTLE PENDING)
    // ----------------------------------------
    @PostMapping("/capture/{accountId}")
    public ResponseEntity<LedgerResponse> captureAccount(
            Authentication auth,
            @PathVariable UUID accountId,
            @Valid @RequestBody AmountRequest captureRequest
    ) {
        LedgerResponse response = ledgerService.capture(userId(auth), accountId, captureRequest);
        return ResponseEntity.ok(response);
    }

    // ----------------------------------------
    // RELEASE (VOID PENDING)
    // ----------------------------------------
    @PostMapping("/release/{accountId}")
    public ResponseEntity<LedgerResponse> releaseAccount(
            Authentication auth,
            @PathVariable UUID accountId,
            @Valid @RequestBody AmountRequest releaseRequest
    ) {
        LedgerResponse response = ledgerService.release(userId(auth), accountId, releaseRequest);
        return ResponseEntity.ok(response);
    }
}
