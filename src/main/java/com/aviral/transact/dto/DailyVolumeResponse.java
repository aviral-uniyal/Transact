package com.aviral.transact.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyVolumeResponse(LocalDate date, long completedTransferCount, BigDecimal totalAmount) {}
