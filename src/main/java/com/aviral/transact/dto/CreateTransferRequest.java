package com.aviral.transact.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateTransferRequest(
        @NotNull @Positive Long fromAccountId,
        @NotNull @Positive Long toAccountId,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal amount
) {}
