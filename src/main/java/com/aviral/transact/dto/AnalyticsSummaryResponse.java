package com.aviral.transact.dto;

import java.math.BigDecimal;

public record AnalyticsSummaryResponse(long accountCount, long completedTransferCount,
                                       BigDecimal totalTransferredAmount, BigDecimal totalAccountBalances) {}
