package com.ironledger.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ResetPasswordRequest {
    private UUID userId;
    private UUID sessionId;
    private String password;
}
