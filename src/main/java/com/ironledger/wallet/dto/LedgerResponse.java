package com.ironledger.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class LedgerResponse {
    private UUID transactionId;
    private String referenceId;
    private String type;
    private Long amount;
    private OffsetDateTime createdAt;
}
