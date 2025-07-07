package com.example.plgsystem.controller;

import com.example.plgsystem.dto.DeliveryRecordDTO;
import com.example.plgsystem.dto.OrderDTO;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.service.OrderService;
import com.example.plgsystem.service.ServeRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    
    private Order order1;
    private Order order2;
    private Vehicle vehicle;
    private ServeRecord serveRecord;
    private UUID serveRecordId;

    @BeforeEach
    public void setUp() {
        // Crear posiciones
        Position position1 = new Position(10, 20);
        Position position2 = new Position(30, 40);
        
        // Crear vehículo
        vehicle = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(new Position(5, 5))
                .build();
        
        // Crear fechas
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime arrivalTime1 = now.minusDays(1);
        LocalDateTime deadlineTime1 = now.plusDays(1);
        LocalDateTime arrivalTime2 = now.minusDays(2);
        LocalDateTime deadlineTime2 = now.plusDays(2);
        
        // Crear órdenes
        order1 = Order.builder()
                .id("O-001")
                .arrivalTime(arrivalTime1)
                .deadlineTime(deadlineTime1)
                .glpRequestM3(100)
                .position(position1)
                .build();
        order1.setRemainingGlpM3(100);
        
        order2 = Order.builder()
                .id("O-002")
                .arrivalTime(arrivalTime2)
                .deadlineTime(deadlineTime2)
                .glpRequestM3(200)
                .position(position2)
                .build();
        order2.setRemainingGlpM3(200);
        
        // Crear registro de entrega
        serveRecordId = UUID.randomUUID();
        serveRecord = new ServeRecord(vehicle, order1, 50, now);
        
        // Establecer ID manualmente
        try {
            java.lang.reflect.Field idField = ServeRecord.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(serveRecord, serveRecordId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }
    }

    @Test
    public void testGetAllOrders() throws Exception {
        // Given
        when(orderService.findAll()).thenReturn(Arrays.asList(order1, order2));

        // When & Then
        mockMvc.perform(get("/api/orders")
                .param("paginated", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("O-001"))
                .andExpect(jsonPath("$[1].id").value("O-002"));
    }
    
    @Test
    public void testGetAllOrders_WithPagination() throws Exception {
        // Given
        Page<Order> orderPage = new PageImpl<>(
                Arrays.asList(order1, order2),
                PageRequest.of(0, 10),
                2
        );

        when(orderService.findAllPaged(any(Pageable.class))).thenReturn(orderPage);

        // When & Then
        mockMvc.perform(get("/api/orders")
                .param("paginated", "true")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("O-001"))
                .andExpect(jsonPath("$.content[1].id").value("O-002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testGetOrderById() throws Exception {
        // Given
        when(orderService.findById("O-001")).thenReturn(Optional.of(order1));

        // When & Then
        mockMvc.perform(get("/api/orders/O-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("O-001"))
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
        when(orderService.findPendingDeliveries()).thenReturn(Collections.singletonList(order1));

        // When & Then
        mockMvc.perform(get("/api/orders")
                .param("paginated", "false")
                .param("pending", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("O-001"));
    }
    
    @Test
    public void testGetPendingOrders_WithPagination() throws Exception {
        // Given
        Page<Order> orderPage = new PageImpl<>(
                Collections.singletonList(order1),
                PageRequest.of(0, 10),
                1
        );

        when(orderService.findPendingDeliveriesPaged(any(Pageable.class))).thenReturn(orderPage);

        // When & Then
        mockMvc.perform(get("/api/orders")
                .param("paginated", "true")
                .param("pending", "true")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("O-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    public void testGetOverdueOrders() throws Exception {
        // Given
        LocalDateTime overdueAt = LocalDateTime.of(2025, 5, 15, 0, 0);
        Order overdueOrder = Order.builder()
                .id("O-003")
                .arrivalTime(LocalDateTime.of(2025, 5, 10, 0, 0))
                .deadlineTime(LocalDateTime.of(2025, 5, 12, 0, 0))  // Due before overdueAt
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();
        overdueOrder.setRemainingGlpM3(100);

        when(orderService.findOverdueOrders(overdueAt)).thenReturn(Collections.singletonList(overdueOrder));

        // When & Then
        mockMvc.perform(get("/api/orders")
                .param("paginated", "false")
                .param("overdueAt", "2025-05-15T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("O-003"));
    }

    @Test
    public void testGetAvailableOrders() throws Exception {
        // Given
        LocalDateTime availableAt = LocalDateTime.of(2025, 5, 15, 0, 0);
        Order availableOrder = Order.builder()
                .id("O-003")
                .arrivalTime(LocalDateTime.of(2025, 5, 10, 0, 0))  // Available before availableAt
                .deadlineTime(LocalDateTime.of(2025, 5, 20, 0, 0))
                .glpRequestM3(100)
                .position(new Position(10, 20))
                .build();
        availableOrder.setRemainingGlpM3(100);

        when(orderService.findAvailableOrders(availableAt)).thenReturn(Collections.singletonList(availableOrder));

        // When & Then
        mockMvc.perform(get("/api/orders")
                .param("paginated", "false")
                .param("availableAt", "2025-05-15T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("O-003"));
    }

    @Test
    public void testCreateOrder() throws Exception {
        // Given
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId("O-003");
        orderDTO.setArrivalTime(LocalDateTime.now().minusDays(1));
        orderDTO.setDeadlineTime(LocalDateTime.now().plusDays(1));
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
                .andExpect(jsonPath("$.id").value("O-003"));
            
        // Verify that the ID was preserved and not auto-generated
        verify(orderService).save(argThat(o -> o.getId().equals("O-003")));
    }

    @Test
    public void testCreateOrderWithoutId() throws Exception {
        // Given
        OrderDTO orderDTO = new OrderDTO();
        // No ID set explicitly
        orderDTO.setArrivalTime(LocalDateTime.now().minusDays(1));
        orderDTO.setDeadlineTime(LocalDateTime.now().plusDays(1));
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
        orderDTO.setId("O-001");
        orderDTO.setArrivalTime(LocalDateTime.now().minusDays(1));
        orderDTO.setDeadlineTime(LocalDateTime.now().plusDays(1));
        orderDTO.setGlpRequestM3(100);
        orderDTO.setPosition(new Position(10, 20));
        orderDTO.setRemainingGlpM3(50); // Partially delivered

        Order updatedOrder = orderDTO.toEntity();
        updatedOrder.setRemainingGlpM3(50); // Make sure the remaining GLP is properly set

        when(orderService.findById("O-001")).thenReturn(Optional.of(order1));
        when(orderService.save(any(Order.class))).thenReturn(updatedOrder);

        // When & Then
        mockMvc.perform(put("/api/orders/O-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("O-001"))
                .andExpect(jsonPath("$.remainingGlpM3").value(50));
    }

    @Test
    public void testUpdateOrderNotFound() throws Exception {
        // Given
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId("nonexistent");
        orderDTO.setArrivalTime(LocalDateTime.now().minusDays(1));
        orderDTO.setDeadlineTime(LocalDateTime.now().plusDays(1));
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
        when(orderService.findById("O-001")).thenReturn(Optional.of(order1));
        doNothing().when(orderService).deleteById("O-001");

        // When & Then
        mockMvc.perform(delete("/api/orders/O-001"))
                .andExpect(status().isNoContent());

        verify(orderService, times(1)).deleteById("O-001");
    }

    @Test
    public void testDeleteOrderNotFound() throws Exception {
        // Given
        when(orderService.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/orders/nonexistent"))
                .andExpect(status().isNotFound());

        verify(orderService, never()).deleteById("nonexistent");
    }

    @Test
    public void testRecordDelivery() throws Exception {
        // Given
        String orderId = "O-001";
        String vehicleId = "V-001";
        int volumeM3 = 50;
        LocalDateTime serveDate = LocalDateTime.now();

        DeliveryRecordDTO deliveryRecordDTO = new DeliveryRecordDTO();
        deliveryRecordDTO.setVehicleId(vehicleId);
        deliveryRecordDTO.setVolumeM3(volumeM3);
        deliveryRecordDTO.setServeDate(serveDate);
        
        when(orderService.recordDelivery(eq(orderId), eq(volumeM3), eq(vehicleId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(serveRecord));

        // When & Then
        mockMvc.perform(post("/api/orders/" + orderId + "/deliver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deliveryRecordDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(vehicleId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.glpVolumeM3").value(volumeM3));
    }

    @Test
    public void testRecordDeliveryOrderNotFound() throws Exception {
        // Given
        String orderId = "nonexistent";
        String vehicleId = "V-001";
        int volumeM3 = 50;

        DeliveryRecordDTO deliveryRecordDTO = new DeliveryRecordDTO();
        deliveryRecordDTO.setVehicleId(vehicleId);
        deliveryRecordDTO.setVolumeM3(volumeM3);
        deliveryRecordDTO.setServeDate(LocalDateTime.now());

        when(orderService.recordDelivery(eq(orderId), eq(volumeM3), eq(vehicleId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/orders/" + orderId + "/deliver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deliveryRecordDTO)))
                .andExpect(status().isNotFound());
    }
} 