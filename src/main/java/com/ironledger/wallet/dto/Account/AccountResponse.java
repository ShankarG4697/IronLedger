package com.ironledger.wallet.dto.Account;


import com.ironledger.wallet.entity.Account;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AccountResponse {

    private UUID id;
    private String currency;
    private Integer status;
    private Long balanceAvailable;
    private Long balancePending;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AccountResponse from(Account a) {
        return AccountResponse.builder()
                .id(a.getId())
                .currency(a.getCurrency())
                .status(a.getStatus())
                .balanceAvailable(a.getBalanceAvailable())
                .balancePending(a.getBalancePending())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
