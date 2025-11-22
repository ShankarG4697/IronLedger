package com.ironledger.wallet.controller;

import com.ironledger.wallet.dto.Account.AccountCreateRequest;
import com.ironledger.wallet.dto.Account.AccountResponse;
import com.ironledger.wallet.service.AccountService;
import com.ironledger.wallet.utils.AuthenticationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    private UUID userId(Authentication authentication) {
        return AuthenticationUtils.resolveUserIdFromAuthentication(authentication);
    }

    // -------------------------------------------------------------------------
    // CREATE ACCOUNT
    // -------------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            Authentication auth,
            @Valid @RequestBody AccountCreateRequest req
    ) {
        return ResponseEntity.ok(accountService.createAccount(userId(auth), req));
    }

    // -------------------------------------------------------------------------
    // LIST USER ACCOUNTS
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication auth) {
        return ResponseEntity.ok(accountService.listAccounts(userId(auth)));
    }

    // -------------------------------------------------------------------------
    // GET SINGLE ACCOUNT
    // -------------------------------------------------------------------------
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(
            Authentication auth,
            @PathVariable UUID accountId
    ) {
        return ResponseEntity.ok(accountService.getAccount(userId(auth), accountId));
    }
}
