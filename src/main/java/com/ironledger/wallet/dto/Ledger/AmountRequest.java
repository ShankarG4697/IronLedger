package com.ironledger.wallet.dto.Ledger;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class AmountRequest {
    @NotNull(message = "Amount is required!")
    @Positive(message = "Amount must be positive!")
    @Max(value = 10_000_000_000L, message = "Amount too large")
    private Long amount;   // minor units

    @NotNull(message = "Currency is required!")
    @Pattern(regexp = "^[A-Z]{3}$",
            message = "Invalid currency format (must be 3-letter ISO code)")
    private String currency;
}
