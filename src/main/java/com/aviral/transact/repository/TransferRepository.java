package com.aviral.transact.repository;

import com.aviral.transact.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long>, JpaSpecificationExecutor<Transfer> {
    Optional<Transfer> findByIdempotencyKey(String key);
    List<Transfer> findTop100ByOrderByCreatedAtDescIdDesc();

    long countByStatus(Transfer.Status status);

    @Query("select coalesce(sum(t.amount), 0) from Transfer t where t.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") Transfer.Status status);

    @Query(value = """
            select (created_at at time zone 'UTC')::date as day,
                   count(*) as transfer_count, coalesce(sum(amount), 0) as total_amount
            from transfers
            where status = 'COMPLETED' and created_at >= :start and created_at < :end
            group by day order by day
            """, nativeQuery = true)
    List<Object[]> dailyVolume(@Param("start") Instant start, @Param("end") Instant end);
}
