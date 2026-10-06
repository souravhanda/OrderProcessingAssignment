package com.example.orders;

import com.example.orders.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired OrderService orders;

    @BeforeEach
    void clearOrders() {
        jdbc.update("DELETE FROM order_items");
        jdbc.update("DELETE FROM orders");
    }

    @Test
    void publishesInteractiveApiDocumentation() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Order Processing API"))
                .andExpect(jsonPath("$.paths['/orders'].post").exists())
                .andExpect(jsonPath("$.paths['/orders/{id}'].get").exists());
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void createsMultiItemOrderAndRetrievesItsDetails() throws Exception {
        String response = mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("alice", "BOOK-001", "PEN-001")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(17.00))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(json.readTree(response).get("id").asText());
        mvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("alice"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(12.50))
                .andExpect(jsonPath("$.items[1].lineTotal").value(4.50));
        mvc.perform(get("/orders").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id.toString()));
        mvc.perform(get("/orders").param("status", "SHIPPED"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsInvalidOrderInputs() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"alice\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"alice\",\"items\":[{\"sku\":\"BOOK-001\",\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(request("alice", "BOOK-001", "BOOK-001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Duplicate SKU: BOOK-001"));
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(request("alice", "UNKNOWN", "PEN-001")))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
    }

    @Test
    void permitsOnlyForwardTransitionsAndPendingCancellation() throws Exception {
        UUID cancellable = create("alice");
        mvc.perform(post("/orders/{id}/cancel", cancellable))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/orders/{id}/cancel", cancellable))
                .andExpect(status().isConflict());

        UUID progressing = create("bob");
        mvc.perform(patch("/orders/{id}/status", progressing)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isConflict());
        for (String next : new String[]{"PROCESSING", "SHIPPED", "DELIVERED"}) {
            mvc.perform(patch("/orders/{id}/status", progressing)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + next + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(next));
        }
        mvc.perform(post("/orders/{id}/cancel", progressing))
                .andExpect(status().isConflict());
    }

    @Test
    void scheduledWorkProcessesOnlyStillPendingOrders() throws Exception {
        UUID first = create("alice");
        UUID second = create("bob");
        UUID cancelled = create("carol");
        mvc.perform(post("/orders/{id}/cancel", cancelled)).andExpect(status().isOk());

        assertThat(orders.processPendingOrders()).isEqualTo(2);
        assertThat(orders.processPendingOrders()).isZero();
        mvc.perform(get("/orders/{id}", first)).andExpect(jsonPath("$.status").value("PROCESSING"));
        mvc.perform(get("/orders/{id}", second)).andExpect(jsonPath("$.status").value("PROCESSING"));
        mvc.perform(get("/orders/{id}", cancelled)).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void returnsUsefulErrorsForMissingAndMalformedRequests() throws Exception {
        mvc.perform(get("/orders/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/orders").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/orders").param("status", "UNKNOWN")).andExpect(status().isBadRequest());
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest());
    }

    private UUID create(String customerId) throws Exception {
        String response = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\",\"items\":[{\"sku\":\"BOOK-001\",\"quantity\":1}]}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        JsonNode tree = json.readTree(response);
        return UUID.fromString(tree.get("id").asText());
    }

    private String request(String customerId, String first, String second) {
        return "{\"customerId\":\"" + customerId + "\",\"items\":["
                + "{\"sku\":\"" + first + "\",\"quantity\":1},"
                + "{\"sku\":\"" + second + "\",\"quantity\":2}]}";
    }
}
