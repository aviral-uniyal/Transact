package com.aviral.transact.service;

import com.aviral.transact.dto.*;
import com.aviral.transact.entity.Account;
import com.aviral.transact.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AccountService {
    private final AccountRepository repository;

    public AccountService(AccountRepository repository) { this.repository = repository; }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        Account account = new Account(request.displayName().trim(), request.initialBalance());
        return AccountResponse.from(repository.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        return repository.findAll().stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long id) {
        return repository.findById(id).map(AccountResponse::from)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
