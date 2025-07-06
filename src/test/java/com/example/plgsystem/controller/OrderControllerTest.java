package com.example.plgsystem.controller;

import com.example.plgsystem.dto.DeliveryRecordDTO;
import com.example.plgsystem.dto.OrderDTO;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.service.OrderService;
import com.example.plgsystem.service.ServeRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.argThat;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ServeRecordService serveRecordService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllOrders() throws Exception {
        // Given
        Order order1 = Order.builder()
                .id("O001")
                .arriveTime(LocalDateTime.now().minusDays(1))
                .dueTime(LocalDateTime.now().plusDays(1))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();

        Order order2 = Order.builder()
                .id("O002")
                .arriveTime(LocalDateTime.now().minusDays(2))
                .dueTime(LocalDateTime.now().plusDays(2))
                .glpRequestM3(200)
                .position(new Position(30, 40))
                .build();

        when(orderService.findAll()).thenReturn(Arrays.asList(order1, order2));

        // When & Then
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("O001"))
                .andExpect(jsonPath("$[1].id").value("O002"));
    }

    @Test
    public void testGetOrderById() throws Exception {
        // Given
        Order order = Order.builder()
                .id("O001")
                .arriveTime(LocalDateTime.now().minusDays(1))
                .dueTime(LocalDateTime.now().plusDays(1))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();

        when(orderService.findById("O001")).thenReturn(Optional.of(order));

        // When & Then
        mockMvc.perform(get("/api/orders/O001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("O001"))
                .andExpect(jsonPath("$.glpRequestM3").value(100));
    }

    @Test
    public void testGetOrderByIdNotFound() throws Exception {
        // Given
        when(orderService.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/orders/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetPendingOrders() throws Exception {
        // Given
        Order order = Order.builder()
                .id("O001")
                .arriveTime(LocalDateTime.now().minusDays(1))
                .dueTime(LocalDateTime.now().plusDays(1))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();

        when(orderService.findPendingDeliveries()).thenReturn(Collections.singletonList(order));

        // When & Then
        mockMvc.perform(get("/api/orders").param("pending", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("O001"));
    }

    @Test
    public void testGetOverdueOrders() throws Exception {
        // Given
        LocalDateTime overdueAt = LocalDateTime.of(2025, 5, 15, 0, 0);
        Order order = Order.builder()
                .id("O001")
                .arriveTime(LocalDateTime.of(2025, 5, 10, 0, 0))
                .dueTime(LocalDateTime.of(2025, 5, 12, 0, 0))  // Due before overdueAt
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();

        when(orderService.findOverdueOrders(overdueAt)).thenReturn(Collections.singletonList(order));

        // When & Then
        mockMvc.perform(get("/api/orders").param("overdueAt", "2025-05-15T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("O001"));
    }

    @Test
    public void testGetAvailableOrders() throws Exception {
        // Given
        LocalDateTime availableAt = LocalDateTime.of(2025, 5, 15, 0, 0);
        Order order = Order.builder()
                .id("O001")
                .arriveTime(LocalDateTime.of(2025, 5, 10, 0, 0))  // Available before availableAt
                .dueTime(LocalDateTime.of(2025, 5, 20, 0, 0))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();

        when(orderService.findAvailableOrders(availableAt)).thenReturn(Collections.singletonList(order));

        // When & Then
        mockMvc.perform(get("/api/orders").param("availableAt", "2025-05-15T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("O001"));
    }

    @Test
    public void testCreateOrder() throws Exception {
        // Given
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId("O001");
        orderDTO.setArriveTime(LocalDateTime.now().minusDays(1));
        orderDTO.setDueTime(LocalDateTime.now().plusDays(1));
        orderDTO.setGlpRequestM3(100);
        orderDTO.setPosition(new Position(10, 20));
        orderDTO.setRemainingGlpM3(100);

        Order order = orderDTO.toEntity();

        when(orderService.save(any(Order.class))).thenReturn(order);

        // When & Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("O001"));
            
        // Verify that the ID was preserved and not auto-generated
        verify(orderService).save(argThat(o -> o.getId().equals("O001")));
    }

    @Test
    public void testCreateOrderWithoutId() throws Exception {
        // Given
        OrderDTO orderDTO = new OrderDTO();
        // No ID set explicitly
        orderDTO.setArriveTime(LocalDateTime.now().minusDays(1));
        orderDTO.setDueTime(LocalDateTime.now().plusDays(1));
        orderDTO.setGlpRequestM3(100);
        orderDTO.setPosition(new Position(10, 20));
        orderDTO.setRemainingGlpM3(100);

        // Mock the behavior of ID generation and saving
        when(orderService.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            return savedOrder;
        });

        // When & Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
            
        // Verify that save was called with an order that had an auto-generated ID
        verify(orderService).save(argThat(order -> order.getId() != null));
    }

    @Test
    public void testUpdateOrder() throws Exception {
        // Given
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId("O001");
        orderDTO.setArriveTime(LocalDateTime.now().minusDays(1));
        orderDTO.setDueTime(LocalDateTime.now().plusDays(1));
        orderDTO.setGlpRequestM3(100);
        orderDTO.setPosition(new Position(10, 20));
        orderDTO.setRemainingGlpM3(50); // Partially delivered

        Order existingOrder = Order.builder()
                .id("O001")
                .arriveTime(LocalDateTime.now().minusDays(1))
                .dueTime(LocalDateTime.now().plusDays(1))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();

        Order updatedOrder = orderDTO.toEntity();
        updatedOrder.setRemainingGlpM3(50); // Make sure the remaining GLP is properly set

        when(orderService.findById("O001")).thenReturn(Optional.of(existingOrder));
        when(orderService.save(any(Order.class))).thenReturn(updatedOrder);

        // When & Then
        mockMvc.perform(put("/api/orders/O001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("O001"))
                .andExpect(jsonPath("$.remainingGlpM3").value(50));
    }

    @Test
    public void testUpdateOrderNotFound() throws Exception {
        // Given
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId("nonexistent");
        orderDTO.setArriveTime(LocalDateTime.now().minusDays(1));
        orderDTO.setDueTime(LocalDateTime.now().plusDays(1));
        orderDTO.setGlpRequestM3(100);
        orderDTO.setPosition(new Position(10, 20));

        when(orderService.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/orders/nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteOrder() throws Exception {
        // Given
        Order order = Order.builder()
                .id("O001")
                .arriveTime(LocalDateTime.now().minusDays(1))
                .dueTime(LocalDateTime.now().plusDays(1))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();

        when(orderService.findById("O001")).thenReturn(Optional.of(order));
        doNothing().when(orderService).deleteById("O001");

        // When & Then
        mockMvc.perform(delete("/api/orders/O001"))
                .andExpect(status().isNoContent());

        verify(orderService, times(1)).deleteById("O001");
    }

    @Test
    public void testDeleteOrderNotFound() throws Exception {
        // Given
        when(orderService.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/orders/nonexistent"))
                .andExpect(status().isNotFound());

        verify(orderService, never()).deleteById(anyString());
    }

    @Test
    public void testRecordDelivery() throws Exception {
        // Given
        String orderId = "O001";
        int deliveredVolumeM3 = 50;
        String vehicleId = "V001";
        LocalDateTime serveDate = LocalDateTime.now();

        DeliveryRecordDTO deliveryRecordDTO = new DeliveryRecordDTO();
        deliveryRecordDTO.setVolumeM3(deliveredVolumeM3);
        deliveryRecordDTO.setVehicleId(vehicleId);
        deliveryRecordDTO.setServeDate(serveDate);

        ServeRecord serveRecord = new ServeRecord(vehicleId, orderId, deliveredVolumeM3, serveDate);

        when(orderService.recordDelivery(eq(orderId), eq(deliveredVolumeM3), eq(vehicleId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(serveRecord));
        when(serveRecordService.save(any(ServeRecord.class))).thenReturn(serveRecord);

        // When & Then
        mockMvc.perform(post("/api/orders/O001/deliver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deliveryRecordDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.vehicleId").value(vehicleId))
                .andExpect(jsonPath("$.volumeM3").value(deliveredVolumeM3));
    }

    @Test
    public void testRecordDeliveryOrderNotFound() throws Exception {
        // Given
        String orderId = "nonexistent";
        DeliveryRecordDTO deliveryRecordDTO = new DeliveryRecordDTO();
        deliveryRecordDTO.setVolumeM3(50);
        deliveryRecordDTO.setVehicleId("V001");
        deliveryRecordDTO.setServeDate(LocalDateTime.now());

        when(orderService.recordDelivery(eq(orderId), eq(50), eq("V001"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/orders/nonexistent/deliver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deliveryRecordDTO)))
                .andExpect(status().isNotFound());
    }
} 