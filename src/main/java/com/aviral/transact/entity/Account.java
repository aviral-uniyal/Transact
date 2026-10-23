package com.aviral.transact.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() {}

    public Account(String displayName, BigDecimal balance) {
        this.displayName = displayName;
        this.balance = balance;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public BigDecimal getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }
}
