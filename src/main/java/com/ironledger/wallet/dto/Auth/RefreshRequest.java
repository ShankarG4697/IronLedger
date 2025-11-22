package com.ironledger.wallet.dto.Auth;

import lombok.Data;

@Data
public class RefreshRequest {
    private String refreshToken;
}
