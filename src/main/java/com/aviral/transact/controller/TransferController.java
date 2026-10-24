package com.aviral.transact.controller;

import com.aviral.transact.dto.*;
import com.aviral.transact.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService service;
    private final com.aviral.transact.service.TransferQueryService queries;

    public TransferController(TransferService service, com.aviral.transact.service.TransferQueryService queries) {
        this.service = service;
        this.queries = queries;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> create(@Valid @RequestBody CreateTransferRequest request,
                                                    @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var result = service.create(request, key);
        return result.created()
                ? ResponseEntity.created(URI.create("/api/transfers/" + result.transfer().id())).body(result.transfer())
                : ResponseEntity.ok(result.transfer());
    }

    @GetMapping
    public Object list(@RequestParam(required = false) Long transactionId,
                       @RequestParam(required = false) Long fromAccountId,
                       @RequestParam(required = false) Long toAccountId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       @RequestParam(defaultValue = "false") boolean paged) {
        if (!paged && transactionId == null && fromAccountId == null && toAccountId == null
                && startDate == null && endDate == null && page == null && size == null) {
            return service.list();
        }
        return queries.search(transactionId, fromAccountId, toAccountId, startDate, endDate, page, size);
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable Long id) { return service.get(id); }
}
