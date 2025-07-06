package com.example.plgsystem.controller;

import com.example.plgsystem.dto.ServeRecordDTO;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.service.ServeRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

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

    @Autowired
    private ObjectMapper objectMapper;
    
    // Utility method to set ID using reflection
    private void setId(ServeRecord serveRecord, Long id) {
        try {
            Field idField = ServeRecord.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(serveRecord, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }
    }

    @Test
    public void testGetAllServeRecords() throws Exception {
        // Given
        ServeRecord record1 = new ServeRecord("V001", "O001", 50, LocalDateTime.now().minusHours(2));
        setId(record1, 1L);
        
        ServeRecord record2 = new ServeRecord("V002", "O002", 100, LocalDateTime.now().minusHours(1));
        setId(record2, 2L);
        
        when(serveRecordService.findAll()).thenReturn(Arrays.asList(record1, record2));

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("paginated", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V002"));
    }
    
    @Test
    public void testGetAllServeRecords_WithPagination() throws Exception {
        // Given
        ServeRecord record1 = new ServeRecord("V001", "O001", 50, LocalDateTime.now().minusHours(2));
        setId(record1, 1L);
        
        ServeRecord record2 = new ServeRecord("V002", "O002", 100, LocalDateTime.now().minusHours(1));
        setId(record2, 2L);
        
        Page<ServeRecord> recordPage = new PageImpl<>(
                Arrays.asList(record1, record2),
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
                .andExpect(jsonPath("$.content[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$.content[1].vehicleId").value("V002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testGetServeRecordById() throws Exception {
        // Given
        ServeRecord record = new ServeRecord("V001", "O001", 50, LocalDateTime.now());
        setId(record, 1L);
        
        when(serveRecordService.findById(1L)).thenReturn(Optional.of(record));

        // When & Then
        mockMvc.perform(get("/api/serve-records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vehicleId").value("V001"))
                .andExpect(jsonPath("$.orderId").value("O001"));
    }

    @Test
    public void testGetServeRecordByIdNotFound() throws Exception {
        // Given
        when(serveRecordService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/serve-records/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetServeRecordsByOrderId() throws Exception {
        // Given
        String orderId = "O001";
        ServeRecord record1 = new ServeRecord("V001", orderId, 30, LocalDateTime.now().minusHours(2));
        setId(record1, 1L);
        
        ServeRecord record2 = new ServeRecord("V002", orderId, 20, LocalDateTime.now().minusHours(1));
        setId(record2, 2L);
        
        when(serveRecordService.findByOrderId(orderId)).thenReturn(Arrays.asList(record1, record2));

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("paginated", "false")
                .param("orderId", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(orderId))
                .andExpect(jsonPath("$[1].orderId").value(orderId));
    }

    @Test
    public void testGetServeRecordsByVehicleId() throws Exception {
        // Given
        String vehicleId = "V001";
        ServeRecord record1 = new ServeRecord(vehicleId, "O001", 30, LocalDateTime.now().minusHours(2));
        setId(record1, 1L);
        
        ServeRecord record2 = new ServeRecord(vehicleId, "O002", 20, LocalDateTime.now().minusHours(1));
        setId(record2, 2L);
        
        when(serveRecordService.findByVehicleId(vehicleId)).thenReturn(Arrays.asList(record1, record2));

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("paginated", "false")
                .param("vehicleId", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value(vehicleId))
                .andExpect(jsonPath("$[1].vehicleId").value(vehicleId));
    }

    @Test
    public void testGetServeRecordsByDateRange() throws Exception {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 5, 31, 23, 59);
        
        ServeRecord record1 = new ServeRecord("V001", "O001", 50, LocalDateTime.of(2025, 5, 15, 10, 0));
        setId(record1, 1L);
        
        ServeRecord record2 = new ServeRecord("V002", "O002", 100, LocalDateTime.of(2025, 5, 20, 14, 30));
        setId(record2, 2L);
        
        when(serveRecordService.findByServeDateBetween(startDate, endDate))
                .thenReturn(Arrays.asList(record1, record2));

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("paginated", "false")
                .param("startDate", "2025-05-01T00:00:00")
                .param("endDate", "2025-05-31T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V002"));
    }
    
    @Test
    public void testGetServeRecordsByOrderId_WithPagination() throws Exception {
        // Given
        String orderId = "O001";
        ServeRecord record1 = new ServeRecord("V001", orderId, 30, LocalDateTime.now().minusHours(2));
        setId(record1, 1L);
        
        ServeRecord record2 = new ServeRecord("V002", orderId, 20, LocalDateTime.now().minusHours(1));
        setId(record2, 2L);
        
        Page<ServeRecord> recordPage = new PageImpl<>(
                Arrays.asList(record1, record2),
                PageRequest.of(0, 10),
                2
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
                .andExpect(jsonPath("$.content[1].orderId").value(orderId))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
    
    @Test
    public void testGetServeRecordsByVehicleId_WithPagination() throws Exception {
        // Given
        String vehicleId = "V001";
        ServeRecord record1 = new ServeRecord(vehicleId, "O001", 30, LocalDateTime.now().minusHours(2));
        setId(record1, 1L);
        
        ServeRecord record2 = new ServeRecord(vehicleId, "O002", 20, LocalDateTime.now().minusHours(1));
        setId(record2, 2L);
        
        Page<ServeRecord> recordPage = new PageImpl<>(
                Arrays.asList(record1, record2),
                PageRequest.of(0, 10),
                2
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
                .andExpect(jsonPath("$.content[1].vehicleId").value(vehicleId))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
    
    @Test
    public void testGetServeRecordsByDateRange_WithPagination() throws Exception {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 5, 31, 23, 59);
        
        ServeRecord record1 = new ServeRecord("V001", "O001", 50, LocalDateTime.of(2025, 5, 15, 10, 0));
        setId(record1, 1L);
        
        ServeRecord record2 = new ServeRecord("V002", "O002", 100, LocalDateTime.of(2025, 5, 20, 14, 30));
        setId(record2, 2L);
        
        Page<ServeRecord> recordPage = new PageImpl<>(
                Arrays.asList(record1, record2),
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
                .andExpect(jsonPath("$.content[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$.content[1].vehicleId").value("V002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testCreateServeRecord() throws Exception {
        // Given
        ServeRecord record = new ServeRecord("V001", "O001", 50, LocalDateTime.now());
        ServeRecordDTO recordDTO = ServeRecordDTO.fromEntity(record);
        
        ServeRecord savedRecord = new ServeRecord("V001", "O001", 50, LocalDateTime.now());
        setId(savedRecord, 1L);
        
        when(serveRecordService.save(any(ServeRecord.class))).thenReturn(savedRecord);

        // When & Then
        mockMvc.perform(post("/api/serve-records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recordDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vehicleId").value("V001"))
                .andExpect(jsonPath("$.orderId").value("O001"));
    }

    @Test
    public void testDeleteServeRecord() throws Exception {
        // Given
        ServeRecord record = new ServeRecord("V001", "O001", 50, LocalDateTime.now());
        setId(record, 1L);
        
        when(serveRecordService.findById(1L)).thenReturn(Optional.of(record));
        doNothing().when(serveRecordService).deleteById(1L);

        // When & Then
        mockMvc.perform(delete("/api/serve-records/1"))
                .andExpect(status().isNoContent());
        
        verify(serveRecordService, times(1)).deleteById(1L);
    }

    @Test
    public void testDeleteServeRecordNotFound() throws Exception {
        // Given
        when(serveRecordService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/serve-records/999"))
                .andExpect(status().isNotFound());
        
        verify(serveRecordService, never()).deleteById(999L);
    }
} 