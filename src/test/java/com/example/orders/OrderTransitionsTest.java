package com.example.orders;

import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.OrderTransitions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTransitionsTest {
    @Test
    void acceptsOnlyTheNextForwardStatus() {
        assertThat(OrderTransitions.canAdvance(OrderStatus.PENDING, OrderStatus.PROCESSING)).isTrue();
        assertThat(OrderTransitions.canAdvance(OrderStatus.PROCESSING, OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderTransitions.canAdvance(OrderStatus.SHIPPED, OrderStatus.DELIVERED)).isTrue();
        assertThat(OrderTransitions.canAdvance(OrderStatus.PENDING, OrderStatus.DELIVERED)).isFalse();
        assertThat(OrderTransitions.canAdvance(OrderStatus.PENDING, OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderTransitions.canAdvance(OrderStatus.DELIVERED, OrderStatus.PENDING)).isFalse();
    }
}
