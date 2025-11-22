package com.ironledger.wallet.dto.Transfer;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class TransferResponse {
    private UUID transferId;
    private String transferStatus;
}
