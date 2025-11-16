package com.ironledger.wallet.controller;

import com.ironledger.wallet.dto.AccountCreateRequest;
import com.ironledger.wallet.dto.AccountResponse;
import com.ironledger.wallet.entity.User;
import com.ironledger.wallet.security.JwtProvider;
import com.ironledger.wallet.service.AccountService;
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
    private final JwtProvider jwtProvider;

    // -------------------------------------------------------------------------
    // CREATE ACCOUNT
    // -------------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            Authentication auth,
            @Valid @RequestBody AccountCreateRequest req
    ) {
        UUID userId = resolveUserIdFromAuthentication(auth);
        return ResponseEntity.ok(accountService.createAccount(userId, req));
    }

    // -------------------------------------------------------------------------
    // LIST USER ACCOUNTS
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication auth) {
        UUID userId = resolveUserIdFromAuthentication(auth);
        return ResponseEntity.ok(accountService.listAccounts(userId));
    }

    // -------------------------------------------------------------------------
    // GET SINGLE ACCOUNT
    // -------------------------------------------------------------------------
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(
            Authentication auth,
            @PathVariable UUID accountId
    ) {
        UUID userId = resolveUserIdFromAuthentication(auth);
        return ResponseEntity.ok(accountService.getAccount(userId, accountId));
    }

    // -------------------------------------------------------------------------
    // INTERNAL — Resolve user UUID from Authentication principal
    // Customize based on your security implementation
    // -------------------------------------------------------------------------
    private UUID resolveUserIdFromAuthentication(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Unauthenticated request");
        }

        if (auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }

        throw new IllegalStateException("Unable to resolve userId from principal");
    }
}
