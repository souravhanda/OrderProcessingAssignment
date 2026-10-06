package com.example.orders.api;

import com.example.orders.domain.OrderEntity;
import com.example.orders.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(UUID id, String customerId, OrderStatus status,
                                   String currency, BigDecimal totalAmount,
                                   Instant createdAt, Instant updatedAt) {
    public static OrderSummaryResponse from(OrderEntity order) {
        return new OrderSummaryResponse(order.getId(), order.getCustomerId(), order.getStatus(),
                order.getCurrency(), order.getTotalAmount(), order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
