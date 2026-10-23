package com.aviral.transact.controller;

import com.aviral.transact.dto.*;
import com.aviral.transact.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService service;

    public TransferController(TransferService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<TransferResponse> create(@Valid @RequestBody CreateTransferRequest request,
                                                    @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var result = service.create(request, key);
        return result.created()
                ? ResponseEntity.created(URI.create("/api/transfers/" + result.transfer().id())).body(result.transfer())
                : ResponseEntity.ok(result.transfer());
    }

    @GetMapping
    public List<TransferResponse> list() { return service.list(); }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable Long id) { return service.get(id); }
}
