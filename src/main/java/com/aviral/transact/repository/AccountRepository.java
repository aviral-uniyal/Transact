package com.aviral.transact.repository;

import com.aviral.transact.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findLockedById(Long id);

    @Query("select coalesce(sum(a.balance), 0) from Account a")
    BigDecimal sumBalances();
}
