package com.aviral.transact.dto;

import com.aviral.transact.entity.Transfer;
import java.math.BigDecimal;
import java.time.Instant;

public record StatementEntryResponse(Long transferId, Instant createdAt, String direction,
                                     Long counterpartyAccountId, BigDecimal amount, String status) {
    public static StatementEntryResponse from(Transfer transfer, Long accountId) {
        boolean outgoing = transfer.getFromAccount().getId().equals(accountId);
        return new StatementEntryResponse(transfer.getId(), transfer.getCreatedAt(),
                outgoing ? "DEBIT" : "CREDIT",
                outgoing ? transfer.getToAccount().getId() : transfer.getFromAccount().getId(),
                transfer.getAmount(), transfer.getStatus().name());
    }
}
