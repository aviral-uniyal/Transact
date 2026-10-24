package com.aviral.transact.service;

import com.aviral.transact.dto.*;
import com.aviral.transact.entity.Transfer;
import com.aviral.transact.repository.TransferRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service
public class TransferQueryService {
    private final TransferRepository repository;

    public TransferQueryService(TransferRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public PagedResponse<TransferResponse> search(Long transactionId, Long fromAccountId, Long toAccountId,
                                                   LocalDate startDate, LocalDate endDate, Integer page, Integer size) {
        QueryValidation.positive(transactionId, "transactionId");
        QueryValidation.positive(fromAccountId, "fromAccountId");
        QueryValidation.positive(toAccountId, "toAccountId");
        QueryValidation.dates(startDate, endDate);

        Instant start = startDate == null ? null : startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate == null ? null : endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Specification<Transfer> filter = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (transactionId != null) predicates.add(builder.equal(root.get("id"), transactionId));
            if (fromAccountId != null) predicates.add(builder.equal(root.get("fromAccount").get("id"), fromAccountId));
            if (toAccountId != null) predicates.add(builder.equal(root.get("toAccount").get("id"), toAccountId));
            if (start != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), start));
            if (end != null) predicates.add(builder.lessThan(root.get("createdAt"), end));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return PagedResponse.from(repository.findAll(filter, QueryValidation.page(page, size)), TransferResponse::from);
    }
}
