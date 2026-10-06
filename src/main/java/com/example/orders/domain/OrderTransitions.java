package com.example.orders.domain;

public final class OrderTransitions {
    private OrderTransitions() {
    }

    public static boolean canAdvance(OrderStatus current, OrderStatus requested) {
        return switch (current) {
            case PENDING -> requested == OrderStatus.PROCESSING;
            case PROCESSING -> requested == OrderStatus.SHIPPED;
            case SHIPPED -> requested == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
