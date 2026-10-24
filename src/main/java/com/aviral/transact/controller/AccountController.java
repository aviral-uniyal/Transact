package com.aviral.transact.controller;

import com.aviral.transact.dto.*;
import com.aviral.transact.service.AccountService;
import com.aviral.transact.service.StatementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService service;
    private final StatementService statements;

    public AccountController(AccountService service, StatementService statements) {
        this.service = service;
        this.statements = statements;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = service.create(request);
        return ResponseEntity.created(URI.create("/api/accounts/" + account.id())).body(account);
    }

    @GetMapping
    public List<AccountResponse> list() { return service.list(); }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) { return service.get(id); }

    @GetMapping("/{id}/statement")
    public AccountStatementResponse statement(@PathVariable Long id,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size) {
        return statements.get(id, page, size);
    }
}
