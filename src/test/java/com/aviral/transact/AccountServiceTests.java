package com.aviral.transact;

import com.aviral.transact.dto.CreateAccountRequest;
import com.aviral.transact.entity.Account;
import com.aviral.transact.repository.AccountRepository;
import com.aviral.transact.service.AccountNotFoundException;
import com.aviral.transact.service.AccountService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTests {
    @Test
    void createsAndListsAccounts() {
        AccountRepository repository = mock(AccountRepository.class);
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AccountService service = new AccountService(repository);
        var response = service.create(new CreateAccountRequest("  Demo  ", new BigDecimal("25.00")));
        assertEquals("Demo", response.displayName());
        assertEquals(new BigDecimal("25.00"), response.balance());
        assertNotNull(response.createdAt());
        verify(repository).save(any(Account.class));
    }

    @Test
    void missingAccountIsReported() {
        AccountRepository repository = mock(AccountRepository.class);
        when(repository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> new AccountService(repository).get(42L));
    }

    @Test
    void rejectsNegativeBalanceAndBlankName() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new CreateAccountRequest(" ", new BigDecimal("-1.00"));
            assertEquals(2, validator.validate(request).size());
        }
    }
}
