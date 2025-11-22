package com.ironledger.wallet.controller;

import com.ironledger.wallet.dto.Transfer.*;
import com.ironledger.wallet.service.TransferService;
import com.ironledger.wallet.utils.AuthenticationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {
    private final TransferService transferService;

    private UUID userId(Authentication authentication) {
        return AuthenticationUtils.resolveUserIdFromAuthentication(authentication);
    }

    @PostMapping
    public TransferResponse createTransfer(Authentication auth, @Valid @RequestBody TransferRequest request) {
        return transferService.transfer(userId(auth), request);
    }

    @GetMapping("/{transferId}")
    public TransferView getTransfer(@PathVariable UUID transferId) {
        return transferService.transferGet(transferId);
    }

    @PostMapping("/reversal")
    public ReversalResponse transferReversal(Authentication auth, @RequestBody ReversalRequest request) {
        return transferService.transferReversal(request, userId(auth));
    }
}
