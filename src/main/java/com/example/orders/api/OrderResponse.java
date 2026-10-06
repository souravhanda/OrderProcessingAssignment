package com.example.orders.api;

import com.example.orders.domain.OrderEntity;
import com.example.orders.domain.OrderItemEntity;
import com.example.orders.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, String customerId, OrderStatus status, String currency,
                            BigDecimal totalAmount, Instant createdAt, Instant updatedAt,
                            List<Item> items) {
    public static OrderResponse from(OrderEntity order) {
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus(),
                order.getCurrency(), order.getTotalAmount(), order.getCreatedAt(),
                order.getUpdatedAt(), order.getItems().stream().map(Item::from).toList());
    }

    public record Item(String sku, String productName, int quantity, BigDecimal unitPrice,
                       BigDecimal lineTotal) {
        static Item from(OrderItemEntity item) {
            return new Item(item.getSku(), item.getProductName(), item.getQuantity(),
                    item.getUnitPrice(), item.getLineTotal());
        }
    }
}
