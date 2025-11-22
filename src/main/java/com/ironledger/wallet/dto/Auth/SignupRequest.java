package com.ironledger.wallet.dto.Auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignupRequest {
    private String email;
    private String password;
    private String fullName;
    private String phoneNumber;
}
