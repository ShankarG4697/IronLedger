package com.ironledger.wallet.service;

import com.ironledger.wallet.dto.Transfer.*;
import com.ironledger.wallet.entity.Account;
import com.ironledger.wallet.entity.LedgerTransaction;
import com.ironledger.wallet.entity.Transfer;
import com.ironledger.wallet.exception.InvalidRequestException;
import com.ironledger.wallet.exception.ResourceNotFoundException;
import com.ironledger.wallet.repository.AccountRepository;
import com.ironledger.wallet.repository.LedgerRepository;
import com.ironledger.wallet.repository.TransferRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {
    private final AccountRepository accountRepo;
    private final TransferRepository transferRepo;
    private final LedgerRepository ledgerRepo;
    private final LedgerService ledgerService;

    private static final ZoneId SG_ZONE = ZoneId.of("Asia/Singapore");

    @Transactional
    public TransferResponse transfer(UUID userId, TransferRequest request){
        // 1. Lock sender
        Account sender = accountRepo.findByIdForUpdate(request.getFromAccount())
                .orElseThrow(() -> new ResourceNotFoundException("Sender account not found"));

        // 2. Fetch receiver
        Account receiver = accountRepo.findById(request.getToAccount())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found"));

        if (!sender.getCurrency().equals(receiver.getCurrency()))
            throw new InvalidRequestException("Currency mismatch");

        if (!sender.getUserId().equals(userId))
            throw new ResourceNotFoundException("Sender account not found");

        long available = sender.getBalanceAvailable();

        if (available < request.getAmount())
            throw new InvalidRequestException("Insufficient funds");

        String referenceId = ledgerService.generateReferenceId();

        // 3. Create a transfer row (PENDING)
        Transfer transfer = new Transfer();
        transfer.setFromAccount(sender.getId());
        transfer.setToAccount(receiver.getId());
        transfer.setAmount(request.getAmount());
        transfer.setCurrency(sender.getCurrency());
        transfer.setMeta(request.getMetadata());
        transfer.setCreatedBy(userId);
        transfer.setCreatedAt(OffsetDateTime.now(SG_ZONE));
        transfer.setTransferStatus("PENDING");
        transferRepo.save(transfer);

        // 4. Compute balances before/after
        long senderBefore = sender.getBalanceAvailable();
        long senderAfter = senderBefore - request.getAmount();

        long receiverBefore = receiver.getBalanceAvailable();
        long receiverAfter = receiverBefore + request.getAmount();

        // 5. Create sender DEBIT ledger row (using LedgerService internals)
        LedgerTransaction debitTx = ledgerService.writeLedger(
                sender,
                -request.getAmount(),
                "DEBIT",
                1,
                referenceId,
                sender.getCurrency()
        );
        debitTx.setBalanceBefore(senderBefore);
        debitTx.setBalanceAfter(senderAfter);
        debitTx.setPendingBefore(sender.getBalancePending());
        debitTx.setPendingAfter(sender.getBalancePending());
        ledgerRepo.save(debitTx);

        // 6. Create a receiver CREDIT ledger row
        LedgerTransaction creditTx = ledgerService.writeLedger(
                receiver,
                request.getAmount(),
                "CREDIT",
                1,
                referenceId,
                receiver.getCurrency()
        );
        creditTx.setBalanceBefore(receiverBefore);
        creditTx.setBalanceAfter(receiverAfter);
        creditTx.setPendingBefore(receiver.getBalancePending());
        creditTx.setPendingAfter(receiver.getBalancePending());
        ledgerRepo.save(creditTx);

        // 7. Update account balances
        sender.setBalanceAvailable(senderAfter);
        receiver.setBalanceAvailable(receiverAfter);
        accountRepo.save(sender);
        accountRepo.save(receiver);

        // 8. Mark completed
        transfer.setTransferStatus("COMPLETED");
        transfer.setCompletedAt(OffsetDateTime.now());
        transferRepo.save(transfer);

        return new TransferResponse(transfer.getId(), transfer.getTransferStatus());
    }

    @Transactional
    public TransferView transferGet(UUID transferId) {
        Transfer transfer = transferRepo.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));

        return new TransferView(
                transfer.getId(),
                transfer.getFromAccount(),
                transfer.getToAccount(),
                transfer.getCurrency(),
                transfer.getTransferStatus(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt()
        );
    }

    @Transactional
    public ReversalResponse transferReversal(ReversalRequest request, UUID userId) {
        // 1. Fetch original
        Transfer original = transferRepo.findById(request.getOriginalTransferId())
                .orElseThrow(() -> new ResourceNotFoundException("Original transfer not found"));

        if(!original.getCreatedBy().equals(userId)){
            throw new InvalidRequestException("User is not the creator of the original transfer");
        }

        if(original.getTransferStatus().equals("REVERSED")){
            throw new InvalidRequestException("Transfer has already been reversed");
        }

        UUID senderId = original.getFromAccount();
        UUID receiverId = original.getToAccount();
        long amount = original.getAmount();
        String referenceId = ledgerService.generateReferenceId();

        // 2. Lock receiver (they must have enough balances to return funds)
        Account receiver = accountRepo.findByIdForUpdate(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found"));

        Account sender = accountRepo.findByIdForUpdate(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender account not found"));

        if(receiver.getBalanceAvailable() < amount){
            throw new InvalidRequestException("Insufficient funds");
        }

        // 3. Create reversal transfer
        Transfer reversal = new Transfer();
        reversal.setFromAccount(receiverId);
        reversal.setToAccount(senderId);
        reversal.setAmount(amount);
        reversal.setCurrency(original.getCurrency());
        reversal.setCreatedBy(userId);
        reversal.setCreatedAt(OffsetDateTime.now(SG_ZONE));
        reversal.setTransferStatus("PENDING_REVERSAL");
        reversal.setOriginalTransferId(request.getOriginalTransferId());
        reversal.setMeta(getReversalMetaData(original));
        transferRepo.save(reversal);

        // 4. Compute balances
        long receiverBefore = receiver.getBalanceAvailable();
        long receiverAfter  = receiverBefore - amount;

        long senderBefore = sender.getBalanceAvailable();
        long senderAfter  = senderBefore + amount;

        // 5. Write ledger entries
        LedgerTransaction debit = ledgerService.writeLedger(
                receiver, -amount, "REVERSAL_DEBIT", 1,
                referenceId, receiver.getCurrency()
        );
        debit.setBalanceBefore(receiverBefore);
        debit.setBalanceAfter(receiverAfter);
        ledgerRepo.save(debit);

        LedgerTransaction credit = ledgerService.writeLedger(
                sender, amount, "REVERSAL_CREDIT", 1,
                referenceId, sender.getCurrency()
        );
        credit.setBalanceBefore(senderBefore);
        credit.setBalanceAfter(senderAfter);
        ledgerRepo.save(credit);

        // 6. Update balances
        receiver.setBalanceAvailable(receiverAfter);
        sender.setBalanceAvailable(senderAfter);
        accountRepo.save(receiver);
        accountRepo.save(sender);

        // 7. Finalize statuses
        reversal.setTransferStatus("COMPLETED");
        reversal.setCompletedAt(OffsetDateTime.now());
        transferRepo.save(reversal);

        original.setTransferStatus("REVERSED");
        transferRepo.save(original);

        return new ReversalResponse(reversal.getId(), reversal.getTransferStatus());
    }

    private Map<String, Object> getReversalMetaData(Transfer transfer){
        Map<String, Object> metaData = new HashMap<>();
        metaData.put("action", "REVERSAL");
        metaData.put("original_transfer_id", transfer.getId());
        metaData.put("original_transfer_status", transfer.getTransferStatus());
        metaData.put("original_amount", transfer.getAmount());
        metaData.put("original_currency", transfer.getCurrency());
        metaData.put("original_created_at", transfer.getCreatedAt());
        metaData.put("original_completed_at", transfer.getCompletedAt());
        metaData.put("original_meta", transfer.getMeta());
        metaData.put("original_created_by", transfer.getCreatedBy());
        metaData.put("original_to_account", transfer.getToAccount());
        metaData.put("original_from_account", transfer.getFromAccount());

        return metaData;
    }
}
