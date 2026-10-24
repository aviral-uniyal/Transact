package com.aviral.transact.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;

public final class QueryValidation {
    private QueryValidation() {}

    public static PageRequest page(Integer number, Integer size) {
        int page = number == null ? 0 : number;
        int limit = size == null ? 20 : size;
        if (page < 0 || page > 1_000_000 || limit < 1 || limit > 100) {
            throw new TransferException(HttpStatus.BAD_REQUEST, "page must be 0-1000000 and size must be 1-100");
        }
        return PageRequest.of(page, limit, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    public static void positive(Long id, String name) {
        if (id != null && id <= 0) {
            throw new TransferException(HttpStatus.BAD_REQUEST, name + " must be positive");
        }
    }

    public static void dates(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new TransferException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }
        if (end != null && end.equals(LocalDate.MAX)) {
            throw new TransferException(HttpStatus.BAD_REQUEST, "endDate is out of range");
        }
    }
}
