package com.example.orders.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PendingOrderScheduler {
    private static final Logger log = LoggerFactory.getLogger(PendingOrderScheduler.class);
    private final OrderService orders;

    public PendingOrderScheduler(OrderService orders) {
        this.orders = orders;
    }

    @Scheduled(cron = "${orders.processing.cron}", zone = "UTC")
    public void processPendingOrders() {
        int changed = orders.processPendingOrders();
        log.info("Moved {} pending orders to processing", changed);
    }
}
