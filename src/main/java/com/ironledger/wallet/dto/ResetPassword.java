package com.ironledger.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ResetPassword {
    private UUID userId;
    private String fullName;
    private String email;
}
