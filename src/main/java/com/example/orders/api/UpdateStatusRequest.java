package com.example.orders.api;

import com.example.orders.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@Schema(example = "PROCESSING") @NotNull OrderStatus status) {
}
