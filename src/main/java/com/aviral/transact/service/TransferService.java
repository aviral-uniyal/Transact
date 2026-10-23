package com.aviral.transact.service;

import com.aviral.transact.dto.*;
import com.aviral.transact.entity.*;
import com.aviral.transact.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TransferService {
    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final EntityManager entityManager;

    public TransferService(AccountRepository accounts, TransferRepository transfers, EntityManager entityManager) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.entityManager = entityManager;
    }

    @Transactional
    public TransferResult create(CreateTransferRequest request, String rawKey) {
        String key = rawKey == null ? "" : rawKey.trim();
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new TransferException(HttpStatus.BAD_REQUEST, "Idempotency-Key must be 1-128 letters, digits, dots, underscores, colons, or hyphens");
        }
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new TransferException(HttpStatus.BAD_REQUEST, "Sender and receiver must differ");
        }

        // The transaction-scoped advisory lock serializes requests for this key before lookup.
        // Hash collisions only cause extra waiting; the full key still determines identity.
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))")
                .setParameter("key", key).getSingleResult();

        var existing = transfers.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            Transfer transfer = existing.get();
            if (transfer.getFromAccount().getId().equals(request.fromAccountId())
                    && transfer.getToAccount().getId().equals(request.toAccountId())
                    && transfer.getAmount().compareTo(request.amount()) == 0) {
                return new TransferResult(TransferResponse.from(transfer), false);
            }
            throw new TransferException(HttpStatus.CONFLICT, "Idempotency-Key was used for a different transfer");
        }

        long low = Math.min(request.fromAccountId(), request.toAccountId());
        long high = Math.max(request.fromAccountId(), request.toAccountId());
        Account first = accounts.findLockedById(low)
                .orElseThrow(() -> new AccountNotFoundException(low));
        Account second = accounts.findLockedById(high)
                .orElseThrow(() -> new AccountNotFoundException(high));
        Account sender = request.fromAccountId() == low ? first : second;
        Account receiver = request.toAccountId() == low ? first : second;
        if (sender.getBalance().compareTo(request.amount()) < 0) {
            throw new TransferException(HttpStatus.CONFLICT, "Insufficient funds");
        }
        sender.debit(request.amount());
        receiver.credit(request.amount());
        Transfer transfer = transfers.save(new Transfer(sender, receiver, request.amount(), key));
        return new TransferResult(TransferResponse.from(transfer), true);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> list() {
        return transfers.findTop100ByOrderByCreatedAtDescIdDesc().stream().map(TransferResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TransferResponse get(Long id) {
        return transfers.findById(id).map(TransferResponse::from)
                .orElseThrow(() -> new TransferException(HttpStatus.NOT_FOUND, "Transfer " + id + " was not found"));
    }

    public record TransferResult(TransferResponse transfer, boolean created) {}
}
