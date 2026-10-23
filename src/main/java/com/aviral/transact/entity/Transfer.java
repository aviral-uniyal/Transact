package com.aviral.transact.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfers", uniqueConstraints = @UniqueConstraint(name = "uk_transfers_idempotency_key", columnNames = "idempotency_key"))
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Transfer() {}

    public Transfer(Account fromAccount, Account toAccount, BigDecimal amount, String idempotencyKey) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = Status.COMPLETED;
        this.createdAt = Instant.now();
    }

    public enum Status { COMPLETED }
    public Long getId() { return id; }
    public Account getFromAccount() { return fromAccount; }
    public Account getToAccount() { return toAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
