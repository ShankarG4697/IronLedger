package com.ironledger.wallet.dto.Transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class TransferView {
    private UUID transferId;
    private UUID fromAccount;
    private UUID toAccount;
    private String currency;
    private String transferStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
}
