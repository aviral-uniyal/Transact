package com.aviral.transact.dto;

import com.aviral.transact.entity.Account;
import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(Long id, String displayName, BigDecimal balance, Instant createdAt) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getDisplayName(), account.getBalance(), account.getCreatedAt());
    }
}
