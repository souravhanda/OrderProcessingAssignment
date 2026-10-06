package com.example.orders.repository;

import com.example.orders.domain.OrderEntity;
import com.example.orders.domain.OrderStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    @EntityGraph(attributePaths = "items")
    @Query("select o from OrderEntity o where o.id = :id")
    Optional<OrderEntity> findDetailedById(@Param("id") UUID id);

    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OrderEntity o set o.status = :next, o.updatedAt = :now where o.id = :id and o.status = :expected")
    int updateStatusIfCurrent(@Param("id") UUID id, @Param("expected") OrderStatus expected,
                              @Param("next") OrderStatus next, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OrderEntity o set o.status = :next, o.updatedAt = :now where o.status = :expected")
    int updateAllWithStatus(@Param("expected") OrderStatus expected,
                            @Param("next") OrderStatus next, @Param("now") Instant now);
}
