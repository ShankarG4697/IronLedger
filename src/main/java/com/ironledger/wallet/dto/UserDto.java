package com.ironledger.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

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
