package com.example.plgsystem.controller;

import com.example.plgsystem.dto.ServeRecordDTO;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.service.ServeRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
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
        
        ServeRecord record2 = new ServeRecord("V002", "O002", 75, LocalDateTime.now().minusHours(1));
        setId(record2, 2L);
        
        when(serveRecordService.findAll()).thenReturn(Arrays.asList(record1, record2));

        // When & Then
        mockMvc.perform(get("/api/serve-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V002"));
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
                .andExpect(jsonPath("$.vehicleId").value("V001"))
                .andExpect(jsonPath("$.orderId").value("O001"))
                .andExpect(jsonPath("$.volumeM3").value(50));
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
        ServeRecord record1 = new ServeRecord("V001", "O001", 25, LocalDateTime.now().minusDays(1));
        setId(record1, 1L);
        
        ServeRecord record2 = new ServeRecord("V002", "O001", 25, LocalDateTime.now());
        setId(record2, 2L);
        
        when(serveRecordService.findByOrderId("O001")).thenReturn(Arrays.asList(record1, record2));

        // When & Then
        mockMvc.perform(get("/api/serve-records").param("orderId", "O001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("O001"))
                .andExpect(jsonPath("$[1].orderId").value("O001"));
    }

    @Test
    public void testGetServeRecordsByVehicleId() throws Exception {
        // Given
        ServeRecord record1 = new ServeRecord("V001", "O001", 25, LocalDateTime.now().minusDays(1));
        setId(record1, 1L);
        
        ServeRecord record2 = new ServeRecord("V001", "O002", 50, LocalDateTime.now());
        setId(record2, 2L);
        
        when(serveRecordService.findByVehicleId("V001")).thenReturn(Arrays.asList(record1, record2));

        // When & Then
        mockMvc.perform(get("/api/serve-records").param("vehicleId", "V001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V001"));
    }

    @Test
    public void testGetServeRecordsByDateRange() throws Exception {
        // Given
        LocalDateTime start = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 5, 31, 23, 59);
        
        ServeRecord record1 = new ServeRecord("V001", "O001", 50, LocalDateTime.of(2025, 5, 15, 10, 0));
        setId(record1, 1L);
        
        ServeRecord record2 = new ServeRecord("V002", "O002", 75, LocalDateTime.of(2025, 5, 20, 14, 30));
        setId(record2, 2L);
        
        when(serveRecordService.findByServeDateBetween(start, end))
                .thenReturn(Arrays.asList(record1, record2));

        // When & Then
        mockMvc.perform(get("/api/serve-records")
                .param("startDate", "2025-05-01T00:00:00")
                .param("endDate", "2025-05-31T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V002"));
    }

    @Test
    public void testCreateServeRecord() throws Exception {
        // Given
        ServeRecordDTO serveRecordDTO = new ServeRecordDTO();
        serveRecordDTO.setVehicleId("V001");
        serveRecordDTO.setOrderId("O001");
        serveRecordDTO.setVolumeM3(50);
        serveRecordDTO.setServeDate(LocalDateTime.of(2025, 5, 15, 10, 0));

        ServeRecord savedRecord = new ServeRecord("V001", "O001", 50, LocalDateTime.of(2025, 5, 15, 10, 0));
        setId(savedRecord, 1L);

        when(serveRecordService.save(any(ServeRecord.class))).thenReturn(savedRecord);

        // When & Then
        mockMvc.perform(post("/api/serve-records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(serveRecordDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleId").value("V001"))
                .andExpect(jsonPath("$.orderId").value("O001"))
                .andExpect(jsonPath("$.volumeM3").value(50));
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