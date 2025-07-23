package com.example.plgsystem.controller;

import com.example.plgsystem.dto.ServeRecordDTO;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.service.OrderService;
import com.example.plgsystem.service.ServeRecordService;
import com.example.plgsystem.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServeRecordController.class)
public class ServeRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServeRecordService serveRecordService;
    
    @MockitoBean
    private VehicleService vehicleService;
    
    @MockitoBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;
    
    private Vehicle vehicle1;
    private Vehicle vehicle2;
    private Order order1;
    private Order order2;
    private ServeRecord serveRecord1;
    private ServeRecord serveRecord2;
    private UUID serveRecordId1;
    private LocalDateTime serveDate1;

    @BeforeEach
    public void setUp() {
        // Crear ID fijos para pruebas
        serveRecordId1 = UUID.fromString("a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6");
        UUID serveRecordId2 = UUID.fromString("b2c3d4e5-f6a7-48b9-c0d1-e2f3a4b5c6d7");
        
        // Crear vehículos para pruebas
        vehicle1 = Vehicle.builder()
                .id("V-001")
                .type(VehicleType.TA)
                .currentPosition(new Position(10, 20))
                .build();
        
        vehicle2 = Vehicle.builder()
                .id("V-002")
                .type(VehicleType.TB)
                .currentPosition(new Position(30, 40))
                .build();
        
        // Crear fechas para pruebas
        LocalDateTime now = LocalDateTime.now();
        serveDate1 = now.minusHours(2);
        LocalDateTime serveDate2 = now.minusHours(1);
        
        // Crear órdenes para pruebas
        order1 = Order.builder()
                .id("O-001")
                .arrivalTime(now.minusDays(1))
                .deadlineTime(now.plusDays(1))
                .glpRequestM3(100)
                .position(new Position(15, 25))
                .build();
        
        order2 = Order.builder()
                .id("O-002")
                .arrivalTime(now.minusDays(2))
                .deadlineTime(now.plusDays(2))
                .glpRequestM3(200)
                .position(new Position(35, 45))
                .build();
        
        // Crear registros de entrega para pruebas
        serveRecord1 = new ServeRecord(vehicle1, order1, 50, serveDate1);
        serveRecord2 = new ServeRecord(vehicle2, order2, 100, serveDate2);
        
        // Establecer IDs manualmente
        try {
            java.lang.reflect.Field idField = ServeRecord.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(serveRecord1, serveRecordId1);
            idField.set(serveRecord2, serveRecordId2);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }
        
        // Setup mocks for the dependencies
        when(vehicleService.findById("V-001")).thenReturn(Optional.of(vehicle1));
        when(vehicleService.findById("V-002")).thenReturn(Optional.of(vehicle2));
        when(orderService.findById("O-001")).thenReturn(Optional.of(order1));
        when(orderService.findById("O-002")).thenReturn(Optional.of(order2));
    }

    @Test
    public void testGetAllServeRecords() throws Exception {
        // Given
        when(serveRecordService.findAll()).thenReturn(Arrays.asList(serveRecord1, serveRecord2));

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("paginated", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V-002"));
    }
    
    @Test
    public void testGetAllServeRecords_WithPagination() throws Exception {
        // Given
        Page<ServeRecord> recordPage = new PageImpl<>(
                Arrays.asList(serveRecord1, serveRecord2),
                PageRequest.of(0, 10),
                2
        );
        
        when(serveRecordService.findAllPaged(any(Pageable.class))).thenReturn(recordPage);

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("paginated", "true")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$.content[1].vehicleId").value("V-002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testGetServeRecordById() throws Exception {
        // Given
        when(serveRecordService.findById(serveRecordId1)).thenReturn(Optional.of(serveRecord1));

        // When & Then
        mockMvc.perform(get("/api/serve-records/" + serveRecordId1))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetServeRecordByIdNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(serveRecordService.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/serve-records/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetServeRecordsByOrderId() throws Exception {
        // Given
        String orderId = "O-001";
        when(serveRecordService.findByOrderId(orderId)).thenReturn(Collections.singletonList(serveRecord1));

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("paginated", "false")
                .param("orderId", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(orderId));
    }

    @Test
    public void testGetServeRecordsByVehicleId() throws Exception {
        // Given
        String vehicleId = "V-001";
        when(serveRecordService.findByVehicleId(vehicleId)).thenReturn(Collections.singletonList(serveRecord1));

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("paginated", "false")
                .param("vehicleId", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value(vehicleId));
    }

    @Test
    public void testGetServeRecordsByDateRange() throws Exception {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 5, 31, 23, 59);
        
        ServeRecord rangeRecord1 = new ServeRecord(vehicle1, order1, 50, LocalDateTime.of(2025, 5, 15, 10, 0));
        ServeRecord rangeRecord2 = new ServeRecord(vehicle2, order2, 100, LocalDateTime.of(2025, 5, 20, 14, 30));
        
        // Establecer IDs manualmente
        try {
            java.lang.reflect.Field idField = ServeRecord.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(rangeRecord1, UUID.randomUUID());
            idField.set(rangeRecord2, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }
        
        when(serveRecordService.findByServeDateBetween(startDate, endDate))
                .thenReturn(Arrays.asList(rangeRecord1, rangeRecord2));

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("paginated", "false")
                .param("startDate", "2025-05-01T00:00:00")
                .param("endDate", "2025-05-31T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V-002"));
    }
    
    @Test
    public void testGetServeRecordsByOrderId_WithPagination() throws Exception {
        // Given
        String orderId = "O-001";
        Page<ServeRecord> recordPage = new PageImpl<>(
                Collections.singletonList(serveRecord1),
                PageRequest.of(0, 10),
                1
        );
        
        when(serveRecordService.findByOrderIdPaged(eq(orderId), any(Pageable.class))).thenReturn(recordPage);

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("paginated", "true")
                .param("orderId", orderId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(orderId))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
    
    @Test
    public void testGetServeRecordsByVehicleId_WithPagination() throws Exception {
        // Given
        String vehicleId = "V-001";
        Page<ServeRecord> recordPage = new PageImpl<>(
                Collections.singletonList(serveRecord1),
                PageRequest.of(0, 10),
                1
        );
        
        when(serveRecordService.findByVehicleIdPaged(eq(vehicleId), any(Pageable.class))).thenReturn(recordPage);

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("paginated", "true")
                .param("vehicleId", vehicleId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vehicleId").value(vehicleId))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
    
    @Test
    public void testGetServeRecordsByDateRange_WithPagination() throws Exception {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 5, 31, 23, 59);
        
        ServeRecord rangeRecord1 = new ServeRecord(vehicle1, order1, 50, LocalDateTime.of(2025, 5, 15, 10, 0));
        ServeRecord rangeRecord2 = new ServeRecord(vehicle2, order2, 100, LocalDateTime.of(2025, 5, 20, 14, 30));
        
        // Establecer IDs manualmente
        try {
            java.lang.reflect.Field idField = ServeRecord.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(rangeRecord1, UUID.randomUUID());
            idField.set(rangeRecord2, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }
        
        Page<ServeRecord> recordPage = new PageImpl<>(
                Arrays.asList(rangeRecord1, rangeRecord2),
                PageRequest.of(0, 10),
                2
        );
        
        when(serveRecordService.findByServeDateBetweenPaged(eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(recordPage);

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("paginated", "true")
                .param("startDate", "2025-05-01T00:00:00")
                .param("endDate", "2025-05-31T23:59:00")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$.content[1].vehicleId").value("V-002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
    
    @Test
    public void testCreateServeRecord() throws Exception {
        // Given
        ServeRecordDTO recordDTO = new ServeRecordDTO();
        recordDTO.setVehicleId("V-001");
        recordDTO.setOrderId("O-001");
        recordDTO.setGlpVolumeM3(50);
        recordDTO.setServeDate(serveDate1);
        
        when(serveRecordService.save(any(ServeRecord.class))).thenReturn(serveRecord1);

        // When & Then
        mockMvc.perform(post("/api/serve-records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recordDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(serveRecordId1.toString()))
                .andExpect(jsonPath("$.vehicleId").value("V-001"))
                .andExpect(jsonPath("$.orderId").value("O-001"));
                
        verify(serveRecordService, times(1)).save(any(ServeRecord.class));
    }

    @Test
    public void testDeleteServeRecord() throws Exception {
        // Given
        when(serveRecordService.findById(serveRecordId1)).thenReturn(Optional.of(serveRecord1));
        doNothing().when(serveRecordService).deleteById(serveRecordId1);

        // When & Then
        mockMvc.perform(delete("/api/serve-records/" + serveRecordId1))
                .andExpect(status().isNoContent());
        
        verify(serveRecordService, times(1)).deleteById(serveRecordId1);
    }

    @Test
    public void testDeleteServeRecordNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(serveRecordService.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/serve-records/" + nonExistentId))
                .andExpect(status().isNotFound());
        
        verify(serveRecordService, never()).deleteById(nonExistentId);
    }
} 