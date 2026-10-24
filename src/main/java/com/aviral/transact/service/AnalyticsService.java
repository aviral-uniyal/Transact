package com.aviral.transact.service;

import com.aviral.transact.dto.*;
import com.aviral.transact.entity.Transfer;
import com.aviral.transact.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AnalyticsService {
    private final AccountRepository accounts;
    private final TransferRepository transfers;

    public AnalyticsService(AccountRepository accounts, TransferRepository transfers) {
        this.accounts = accounts;
        this.transfers = transfers;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summary() {
        return new AnalyticsSummaryResponse(accounts.count(), transfers.countByStatus(Transfer.Status.COMPLETED),
                transfers.sumAmountByStatus(Transfer.Status.COMPLETED), accounts.sumBalances());
    }

    @Transactional(readOnly = true)
    public List<DailyVolumeResponse> dailyVolume(LocalDate requestedStart, LocalDate requestedEnd) {
        LocalDate end = requestedEnd == null ? LocalDate.now(ZoneOffset.UTC) : requestedEnd;
        LocalDate start = requestedStart == null ? end.minusDays(6) : requestedStart;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) > 365 || end.equals(LocalDate.MAX)) {
            throw new TransferException(HttpStatus.BAD_REQUEST, "Date range must contain 1-366 days in ascending order");
        }
        Instant from = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant until = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Map<LocalDate, DailyVolumeResponse> byDay = new HashMap<>();
        for (Object[] row : transfers.dailyVolume(from, until)) {
            LocalDate date = row[0] instanceof LocalDate localDate ? localDate : ((java.sql.Date) row[0]).toLocalDate();
            byDay.put(date, new DailyVolumeResponse(date, ((Number) row[1]).longValue(), (BigDecimal) row[2]));
        }
        List<DailyVolumeResponse> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            result.add(byDay.getOrDefault(date, new DailyVolumeResponse(date, 0, BigDecimal.ZERO)));
        }
        return result;
    }
}
