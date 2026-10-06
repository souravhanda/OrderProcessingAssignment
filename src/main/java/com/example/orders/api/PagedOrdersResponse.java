package com.example.orders.api;

import java.util.List;
import org.springframework.data.domain.Page;

public record PagedOrdersResponse(List<OrderSummaryResponse> content, int page, int size,
                                  long totalElements, int totalPages) {
    public static PagedOrdersResponse from(Page<OrderSummaryResponse> result) {
        return new PagedOrdersResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
