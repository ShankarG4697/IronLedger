package com.ironledger.wallet.dto.Auth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class SignupResponse {
    private UUID userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String role;
    private String status;
}
