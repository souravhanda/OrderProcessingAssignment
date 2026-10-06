package com.example.orders;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(info = @Info(
        title = "Order Processing API",
        version = "1.0.0",
        description = "Create and track orders. Seeded SKUs: BOOK-001, PEN-001, BAG-001. "
                + "Pending orders advance to PROCESSING at each five-minute UTC scheduler run."))
public class OrderProcessingApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderProcessingApplication.class, args);
    }
}
