package com.aviral.transact.dto;

import com.aviral.transact.entity.Transfer;
import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(Long id, Long fromAccountId, Long toAccountId,
                               BigDecimal amount, String status, Instant createdAt) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(transfer.getId(), transfer.getFromAccount().getId(),
                transfer.getToAccount().getId(), transfer.getAmount(),
                transfer.getStatus().name(), transfer.getCreatedAt());
    }
}
