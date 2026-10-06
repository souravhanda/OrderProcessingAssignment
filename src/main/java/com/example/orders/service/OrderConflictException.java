package com.example.orders.service;

public class OrderConflictException extends RuntimeException {
    public OrderConflictException(String message) {
        super(message);
    }
}
