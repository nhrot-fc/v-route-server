package com.example.plgsystem.controller;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Order testOrder;

    @BeforeEach
    public void setup() {
        // Create a test order
        Position position = new Position(30, 40);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deliveryDue = now.plusHours(4);
        
        testOrder = new Order("ORD-TEST-001", now, deliveryDue, 800.0, position);
        testOrder = orderRepository.save(testOrder);
    }

    @Test
    public void testGetAllOrders() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].id", is("ORD-TEST-001")))
                .andExpect(jsonPath("$[0].glpRequest", is(800.0)));
    }

    @Test
    public void testGetOrderById() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", testOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testOrder.getId())))
                .andExpect(jsonPath("$.glpRequest", is(800.0)));
        
        // Test with non-existent ID
        mockMvc.perform(get("/api/orders/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetPendingOrders() throws Exception {
        // Create a completed order
        Position pos2 = new Position(50, 60);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due2 = now.plusHours(6);
        
        Order completedOrder = new Order("ORD-COMP-001", now, due2, 500.0, pos2);
        completedOrder.setRemainingGLP(0.0); // Fully delivered
        orderRepository.save(completedOrder);
        
        // Test finding pending orders
        mockMvc.perform(get("/api/orders/pending"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].id", is("ORD-TEST-001")));
    }

    @Test
    public void testGetOrdersByDueTime() throws Exception {
        // Create orders with different due times
        Position pos = new Position(40, 50);
        LocalDateTime now = LocalDateTime.now();
        
        // Order due in 2 hours
        Order urgent = new Order("ORD-URGENT", now, now.plusHours(2), 300.0, pos);
        orderRepository.save(urgent);
        
        // Order due in 8 hours
        Order later = new Order("ORD-LATER", now, now.plusHours(8), 400.0, pos);
        orderRepository.save(later);
        
        // Test finding orders due in next 3 hours
        String dueTime = now.plusHours(3).toString();
        
        mockMvc.perform(get("/api/orders/due-by")
                .param("time", dueTime))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].id", is("ORD-URGENT")));
    }

    @Test
    public void testGetOrdersByDateRange() throws Exception {
        // Create orders from different days
        Position pos = new Position(60, 70);
        LocalDateTime now = LocalDateTime.now();
        
        // Yesterday's order
        LocalDateTime yesterday = now.minusDays(1);
        Order yesterdayOrder = new Order("ORD-YESTERDAY", yesterday, yesterday.plusHours(4), 600.0, pos);
        orderRepository.save(yesterdayOrder);
        
        // Tomorrow's order
        LocalDateTime tomorrow = now.plusDays(1);
        Order tomorrowOrder = new Order("ORD-TOMORROW", tomorrow, tomorrow.plusHours(4), 700.0, pos);
        orderRepository.save(tomorrowOrder);
        
        // Test finding orders from a 3-day range
        String startDate = yesterday.minusHours(1).toString();
        String endDate = tomorrow.plusHours(1).toString();
        
        mockMvc.perform(get("/api/orders/date-range")
                .param("startDate", startDate)
                .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(3))))
                .andExpect(jsonPath("$[*].id", hasItems("ORD-TEST-001", "ORD-YESTERDAY", "ORD-TOMORROW")));
        
        // Test finding only today's orders
        String startToday = now.withHour(0).withMinute(0).toString();
        String endToday = now.withHour(23).withMinute(59).toString();
        
        mockMvc.perform(get("/api/orders/date-range")
                .param("startDate", startToday)
                .param("endDate", endToday))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].id", is("ORD-TEST-001")));
    }

    @Test
    public void testGetOrdersByRadius() throws Exception {
        // Create orders at different locations
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = now.plusHours(4);
        
        // Order near the center
        Position pos1 = new Position(55, 55);
        Order nearOrder = new Order("ORD-NEAR", now, due, 200.0, pos1);
        orderRepository.save(nearOrder);
        
        // Order far from the center
        Position pos2 = new Position(80, 80);
        Order farOrder = new Order("ORD-FAR", now, due, 300.0, pos2);
        orderRepository.save(farOrder);
        
        // Test finding orders within 10 distance units from center
        mockMvc.perform(get("/api/orders/radius")
                .param("x", "50")
                .param("y", "50")
                .param("radius", "10.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].id", is("ORD-NEAR")));
        
        // Test finding orders within 50 distance units from center
        mockMvc.perform(get("/api/orders/radius")
                .param("x", "50")
                .param("y", "50")
                .param("radius", "50.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(3))));
    }

    @Test
    public void testCreateOrder() throws Exception {
        // Create a new order
        Position pos = new Position(90, 100);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = now.plusHours(6);
        
        Order newOrder = new Order("ORD-NEW-001", now, due, 900.0, pos);
        
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("ORD-NEW-001")))
                .andExpect(jsonPath("$.glpRequest", is(900.0)))
                .andExpect(jsonPath("$.remainingGLP", is(900.0)));
    }

    @Test
    public void testUpdateOrderDelivery() throws Exception {
        // Deliver part of the order
        double deliveredAmount = 300.0;
        
        mockMvc.perform(put("/api/orders/{id}/deliver", testOrder.getId())
                .param("amount", String.valueOf(deliveredAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testOrder.getId())))
                .andExpect(jsonPath("$.glpRequest", is(800.0)))
                .andExpect(jsonPath("$.remainingGLP", is(500.0)));
        
        // Deliver the rest
        mockMvc.perform(put("/api/orders/{id}/deliver", testOrder.getId())
                .param("amount", "500.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingGLP", is(0.0)));
        
        // Try to deliver more than remaining
        mockMvc.perform(put("/api/orders/{id}/deliver", testOrder.getId())
                .param("amount", "100.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteOrder() throws Exception {
        mockMvc.perform(delete("/api/orders/{id}", testOrder.getId()))
                .andExpect(status().isOk());
        
        // Verify it's deleted
        mockMvc.perform(get("/api/orders/{id}", testOrder.getId()))
                .andExpect(status().isNotFound());
        
        // Try to delete non-existent order
        mockMvc.perform(delete("/api/orders/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }
}
