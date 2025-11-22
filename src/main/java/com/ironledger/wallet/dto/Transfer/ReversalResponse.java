package com.ironledger.wallet.dto.Transfer;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ReversalResponse {
    private UUID reversalTransferId;
    private String reversalStatus;
}
