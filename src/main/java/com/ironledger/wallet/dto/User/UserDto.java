package com.ironledger.wallet.dto.User;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserDto {
    private UUID userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private String status;
    private UUID sessionId;
}
