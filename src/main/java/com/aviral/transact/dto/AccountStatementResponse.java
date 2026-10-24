package com.aviral.transact.dto;

import java.math.BigDecimal;

public record AccountStatementResponse(Long accountId, String displayName, BigDecimal currentBalance,
                                       PagedResponse<StatementEntryResponse> entries) {}
