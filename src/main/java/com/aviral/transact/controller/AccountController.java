package com.aviral.transact.controller;

import com.aviral.transact.dto.*;
import com.aviral.transact.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = service.create(request);
        return ResponseEntity.created(URI.create("/api/accounts/" + account.id())).body(account);
    }

    @GetMapping
    public List<AccountResponse> list() { return service.list(); }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) { return service.get(id); }
}
