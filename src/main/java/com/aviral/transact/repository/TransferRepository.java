package com.aviral.transact.repository;

import com.aviral.transact.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByIdempotencyKey(String key);
    List<Transfer> findTop100ByOrderByCreatedAtDescIdDesc();
}
