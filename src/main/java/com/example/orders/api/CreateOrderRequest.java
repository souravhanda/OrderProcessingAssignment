package com.example.orders.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateOrderRequest(
        @Schema(example = "customer-123") @NotBlank @Size(max = 100) String customerId,
        @NotEmpty List<@NotNull @Valid Item> items) {

    public record Item(@Schema(example = "BOOK-001") @NotBlank @Size(max = 64) String sku,
                       @Schema(example = "2") @Min(1) int quantity) {
    }
}
