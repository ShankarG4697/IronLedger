package com.ironledger.wallet.dto.Auth;

import com.ironledger.wallet.dto.User.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private UserDto user;
    private String accessToken;
    private String refreshToken;
}
