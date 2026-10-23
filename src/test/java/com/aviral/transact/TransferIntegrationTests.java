package com.aviral.transact;

import com.aviral.transact.dto.*;
import com.aviral.transact.repository.*;
import com.aviral.transact.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfSystemProperty(named = "transact.integration", matches = "true")
class TransferIntegrationTests {
    @Autowired AccountService accountService;
    @Autowired TransferService transferService;
    @Autowired AccountRepository accountRepository;
    @Autowired TransferRepository transferRepository;

    private Long account(String balance) {
        return accountService.create(new CreateAccountRequest("Integration test", new BigDecimal(balance))).id();
    }

    private BigDecimal balance(Long id) { return accountRepository.findById(id).orElseThrow().getBalance(); }

    @Test
    void transferReplayHistoryAndRejections() {
        Long from = account("100.00"), to = account("0.00");
        String key = UUID.randomUUID().toString();
        var request = new CreateTransferRequest(from, to, new BigDecimal("25.0"));
        var created = transferService.create(request, key);
        assertTrue(created.created());
        assertEquals(new BigDecimal("75.00"), balance(from));
        assertEquals(new BigDecimal("25.00"), balance(to));

        var replay = transferService.create(new CreateTransferRequest(from, to, new BigDecimal("25.00")), key);
        assertFalse(replay.created());
        assertEquals(created.transfer().id(), replay.transfer().id());
        assertEquals(new BigDecimal("75.00"), balance(from));
        assertEquals(0, transferService.get(created.transfer().id()).amount().compareTo(new BigDecimal("25.00")));
        assertTrue(transferService.list().stream().anyMatch(t -> t.id().equals(created.transfer().id())));

        assertThrows(TransferException.class, () -> transferService.create(
                new CreateTransferRequest(from, to, new BigDecimal("26.00")), key));
        assertThrows(TransferException.class, () -> transferService.create(
                new CreateTransferRequest(from, from, BigDecimal.ONE), UUID.randomUUID().toString()));
        String failedKey = UUID.randomUUID().toString();
        assertThrows(TransferException.class, () -> transferService.create(
                new CreateTransferRequest(from, to, new BigDecimal("100.00")), failedKey));
        assertEquals(new BigDecimal("75.00"), balance(from));
        assertEquals(new BigDecimal("25.00"), balance(to));
        transferService.create(new CreateTransferRequest(to, from, new BigDecimal("25.00")), UUID.randomUUID().toString());
        assertTrue(transferService.create(new CreateTransferRequest(from, to, new BigDecimal("100.00")), failedKey).created());
    }

    @Test
    void concurrentOverlappingTransfersPreserveBalances() throws Exception {
        Long from = account("100.00"), to = account("0.00");
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                String key = UUID.randomUUID().toString();
                tasks.add(() -> {
                    transferService.create(new CreateTransferRequest(from, to, new BigDecimal("10.00")), key);
                    return null;
                });
            }
            for (Future<Void> future : pool.invokeAll(tasks)) future.get();
            assertEquals(new BigDecimal("0.00"), balance(from));
            assertEquals(new BigDecimal("100.00"), balance(to));
            assertThrows(TransferException.class, () -> transferService.create(
                    new CreateTransferRequest(from, to, BigDecimal.ONE), UUID.randomUUID().toString()));
        } finally { pool.shutdownNow(); }
    }

    @Test
    void concurrentSameKeyTransfersOnce() throws Exception {
        Long from = account("100.00"), to = account("0.00");
        String key = UUID.randomUUID().toString();
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Callable<TransferService.TransferResult>> tasks = new ArrayList<>();
            for (int i = 0; i < 6; i++) tasks.add(() -> transferService.create(
                    new CreateTransferRequest(from, to, new BigDecimal("10.00")), key));
            var results = pool.invokeAll(tasks);
            Set<Long> ids = new HashSet<>();
            int created = 0;
            for (var result : results) {
                var value = result.get();
                ids.add(value.transfer().id());
                if (value.created()) created++;
            }
            assertEquals(1, created);
            assertEquals(1, ids.size());
            assertEquals(new BigDecimal("90.00"), balance(from));
            assertEquals(new BigDecimal("10.00"), balance(to));
            assertEquals(ids.iterator().next(), transferRepository.findByIdempotencyKey(key).orElseThrow().getId());
        } finally { pool.shutdownNow(); }
    }
}
