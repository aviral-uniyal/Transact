package com.aviral.transact.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal initialBalance
) {}
