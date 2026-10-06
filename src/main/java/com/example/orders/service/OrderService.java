package com.example.orders.service;

import com.example.orders.api.CreateOrderRequest;
import com.example.orders.api.OrderResponse;
import com.example.orders.api.OrderSummaryResponse;
import com.example.orders.domain.OrderEntity;
import com.example.orders.domain.OrderItemEntity;
import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.OrderTransitions;
import com.example.orders.domain.ProductEntity;
import com.example.orders.repository.OrderRepository;
import com.example.orders.repository.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final Clock clock;

    public OrderService(OrderRepository orders, ProductRepository products) {
        this.orders = orders;
        this.products = products;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Set<String> skus = new HashSet<>();
        for (CreateOrderRequest.Item item : request.items()) {
            if (!skus.add(item.sku())) {
                throw new InvalidOrderException("Duplicate SKU: " + item.sku());
            }
        }

        Map<String, ProductEntity> bySku = new HashMap<>();
        products.findAllById(skus).forEach(product -> bySku.put(product.getSku(), product));
        String currency = null;
        for (CreateOrderRequest.Item item : request.items()) {
            ProductEntity product = bySku.get(item.sku());
            if (product == null || !product.isActive()) {
                throw new InvalidOrderException("Unknown or inactive SKU: " + item.sku());
            }
            if (currency != null && !currency.equals(product.getCurrency())) {
                throw new InvalidOrderException("All items must use the same currency");
            }
            currency = product.getCurrency();
        }

        OrderEntity order = new OrderEntity(request.customerId(), currency, Instant.now(clock));
        for (CreateOrderRequest.Item item : request.items()) {
            order.addItem(new OrderItemEntity(bySku.get(item.sku()), item.quantity()));
        }
        orders.save(order);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id) {
        return OrderResponse.from(orders.findDetailedById(id)
                .orElseThrow(() -> new OrderNotFoundException(id)));
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> list(OrderStatus status, Pageable pageable) {
        Page<OrderEntity> page = status == null
                ? orders.findAll(pageable)
                : orders.findByStatus(status, pageable);
        return page.map(OrderSummaryResponse::from);
    }

    @Transactional
    public OrderResponse advance(UUID id, OrderStatus requested) {
        OrderStatus current = orders.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id)).getStatus();
        if (!OrderTransitions.canAdvance(current, requested)) {
            throw new OrderConflictException("Cannot change order from " + current + " to " + requested);
        }
        if (orders.updateStatusIfCurrent(id, current, requested, Instant.now(clock)) != 1) {
            throw new OrderConflictException("Order status changed concurrently; retry with its current status");
        }
        return get(id);
    }

    @Transactional
    public OrderResponse cancel(UUID id) {
        OrderStatus current = orders.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id)).getStatus();
        if (current != OrderStatus.PENDING) {
            throw new OrderConflictException("Only PENDING orders can be cancelled");
        }
        if (orders.updateStatusIfCurrent(id, OrderStatus.PENDING, OrderStatus.CANCELLED,
                Instant.now(clock)) != 1) {
            throw new OrderConflictException("Order is no longer PENDING");
        }
        return get(id);
    }

    @Transactional
    public int processPendingOrders() {
        return orders.updateAllWithStatus(OrderStatus.PENDING, OrderStatus.PROCESSING,
                Instant.now(clock));
    }
}
