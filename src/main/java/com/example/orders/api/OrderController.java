package com.example.orders.api;

import com.example.orders.domain.OrderStatus;
import com.example.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Create, inspect, advance, and cancel orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @Operation(summary = "Create an order", description = "Supply one or more seeded product SKUs and quantities; prices come from the catalog.")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orders.create(request);
        return ResponseEntity.created(URI.create("/orders/" + order.id())).body(order);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details")
    public OrderResponse get(@PathVariable UUID id) {
        return orders.get(id);
    }

    @GetMapping
    @Operation(summary = "List orders", description = "Optionally filter by status. Pages are zero-based and limited to 100 orders.")
    public PagedOrdersResponse list(@RequestParam(required = false) OrderStatus status,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be >= 0 and size must be between 1 and 100");
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PagedOrdersResponse.from(orders.list(status, pageable));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Advance an order", description = "Allowed steps: PENDING to PROCESSING to SHIPPED to DELIVERED.")
    public OrderResponse advance(@PathVariable UUID id,
                                 @Valid @RequestBody UpdateStatusRequest request) {
        return orders.advance(id, request.status());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending order")
    public OrderResponse cancel(@PathVariable UUID id) {
        return orders.cancel(id);
    }
}
