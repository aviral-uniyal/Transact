package com.aviral.transact;

import com.aviral.transact.dto.*;
import com.aviral.transact.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfSystemProperty(named = "transact.integration", matches = "true")
class ReadApiIntegrationTests {
    @Autowired AccountService accounts;
    @Autowired TransferService transfers;
    @Autowired TransferQueryService queries;
    @Autowired StatementService statements;
    @Autowired AnalyticsService analytics;

    @Test
    void filteringPaginationAndStatements() {
        Long sender = accounts.create(new CreateAccountRequest("Search sender", new BigDecimal("50.00"))).id();
        Long receiver = accounts.create(new CreateAccountRequest("Search receiver", BigDecimal.ZERO)).id();
        Long unrelated = accounts.create(new CreateAccountRequest("Unrelated", BigDecimal.ZERO)).id();
        Long outgoing = transfers.create(new CreateTransferRequest(sender, receiver, new BigDecimal("10.00")), UUID.randomUUID().toString()).transfer().id();
        Long incoming = transfers.create(new CreateTransferRequest(receiver, sender, new BigDecimal("4.00")), UUID.randomUUID().toString()).transfer().id();
        Long laterOutgoing = transfers.create(new CreateTransferRequest(sender, receiver, BigDecimal.ONE), UUID.randomUUID().toString()).transfer().id();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        var exact = queries.search(outgoing, sender, receiver, today, today, 0, 20);
        assertEquals(1, exact.totalElements());
        assertEquals(outgoing, exact.content().getFirst().id());
        assertEquals(0, queries.search(outgoing, unrelated, null, null, null, 0, 20).totalElements());
        assertEquals(0, queries.search(Long.MAX_VALUE, null, null, null, null, 0, 20).totalElements());
        assertThrows(TransferException.class, () -> queries.search(null, null, null, today, today.minusDays(1), 0, 20));
        assertThrows(TransferException.class, () -> queries.search(null, null, null, null, null, -1, 20));

        var firstPage = queries.search(null, sender, null, null, null, 0, 1);
        var secondPage = queries.search(null, sender, null, null, null, 1, 1);
        assertEquals(2, firstPage.totalElements());
        assertEquals(1, firstPage.size());
        assertEquals(laterOutgoing, firstPage.content().getFirst().id());
        assertEquals(outgoing, secondPage.content().getFirst().id());

        var statement = statements.get(sender, 0, 20);
        assertEquals(sender, statement.accountId());
        assertEquals(new BigDecimal("43.00"), statement.currentBalance());
        assertTrue(statement.entries().content().stream().anyMatch(e -> e.transferId().equals(outgoing)
                && e.direction().equals("DEBIT") && e.counterpartyAccountId().equals(receiver)));
        assertTrue(statement.entries().content().stream().anyMatch(e -> e.transferId().equals(incoming)
                && e.direction().equals("CREDIT") && e.counterpartyAccountId().equals(receiver)));
        assertThrows(AccountNotFoundException.class, () -> statements.get(Long.MAX_VALUE, 0, 20));
        assertEquals(0, statements.get(unrelated, 0, 20).entries().totalElements());
    }

    @Test
    void summaryAndDailyVolumeUsePersistedRows() {
        var before = analytics.summary();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        var dailyBefore = analytics.dailyVolume(today, today).getFirst();
        Long sender = accounts.create(new CreateAccountRequest("Analytics sender", new BigDecimal("30.00"))).id();
        Long receiver = accounts.create(new CreateAccountRequest("Analytics receiver", BigDecimal.ZERO)).id();
        transfers.create(new CreateTransferRequest(sender, receiver, new BigDecimal("7.00")), UUID.randomUUID().toString());

        var after = analytics.summary();
        assertEquals(before.accountCount() + 2, after.accountCount());
        assertEquals(before.completedTransferCount() + 1, after.completedTransferCount());
        assertEquals(0, after.totalTransferredAmount().subtract(before.totalTransferredAmount()).compareTo(new BigDecimal("7.00")));
        assertEquals(0, after.totalAccountBalances().subtract(before.totalAccountBalances()).compareTo(new BigDecimal("30.00")));
        var dailyAfter = analytics.dailyVolume(today, today).getFirst();
        assertEquals(dailyBefore.completedTransferCount() + 1, dailyAfter.completedTransferCount());
        assertEquals(0, dailyAfter.totalAmount().subtract(dailyBefore.totalAmount()).compareTo(new BigDecimal("7.00")));
        assertThrows(TransferException.class, () -> analytics.dailyVolume(today, today.minusDays(1)));
        assertThrows(TransferException.class, () -> analytics.dailyVolume(today.minusDays(400), today));
    }
}
