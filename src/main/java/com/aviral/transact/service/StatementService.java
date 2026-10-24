package com.aviral.transact.service;

import com.aviral.transact.dto.*;
import com.aviral.transact.entity.Transfer;
import com.aviral.transact.repository.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatementService {
    private final AccountRepository accounts;
    private final TransferRepository transfers;

    public StatementService(AccountRepository accounts, TransferRepository transfers) {
        this.accounts = accounts;
        this.transfers = transfers;
    }

    @Transactional(readOnly = true)
    public AccountStatementResponse get(Long accountId, Integer page, Integer size) {
        QueryValidation.positive(accountId, "accountId");
        var account = accounts.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
        Specification<Transfer> involved = (root, query, builder) -> builder.or(
                builder.equal(root.get("fromAccount").get("id"), accountId),
                builder.equal(root.get("toAccount").get("id"), accountId));
        var entries = PagedResponse.from(transfers.findAll(involved, QueryValidation.page(page, size)),
                transfer -> StatementEntryResponse.from(transfer, accountId));
        return new AccountStatementResponse(account.getId(), account.getDisplayName(), account.getBalance(), entries);
    }
}
