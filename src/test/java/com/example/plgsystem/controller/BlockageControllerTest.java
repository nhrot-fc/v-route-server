package com.example.plgsystem.controller;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.service.BlockageService;
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
import java.util.Collections;
import java.util.Optional;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BlockageController.class)
public class BlockageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlockageService blockageService;

    @Autowired
    private ObjectMapper objectMapper;
    
    // Utility method to set ID using reflection
    private void setId(Blockage blockage, Long id) {
        try {
            Field idField = Blockage.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(blockage, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }
    }

    @Test
    public void testGetAllBlockages() throws Exception {
        // Given
        LocalDateTime startTime1 = LocalDateTime.of(2025, 5, 1, 8, 0);
        LocalDateTime endTime1 = LocalDateTime.of(2025, 5, 1, 12, 0);
        List<Position> blockagePoints1 = Arrays.asList(
                new Position(10, 20),
                new Position(10, 21),
                new Position(10, 22)
        );
        Blockage blockage1 = new Blockage(startTime1, endTime1, blockagePoints1);
        setId(blockage1, 1L);
        
        LocalDateTime startTime2 = LocalDateTime.of(2025, 5, 2, 14, 0);
        LocalDateTime endTime2 = LocalDateTime.of(2025, 5, 2, 18, 0);
        List<Position> blockagePoints2 = Arrays.asList(
                new Position(30, 40),
                new Position(31, 40),
                new Position(32, 40)
        );
        Blockage blockage2 = new Blockage(startTime2, endTime2, blockagePoints2);
        setId(blockage2, 2L);
        
        when(blockageService.findAll()).thenReturn(Arrays.asList(blockage1, blockage2));

        // When & Then
        mockMvc.perform(get("/api/blockages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    public void testGetBlockageById() throws Exception {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2025, 5, 1, 8, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 5, 1, 12, 0);
        List<Position> blockagePoints = Arrays.asList(
                new Position(10, 20),
                new Position(10, 21),
                new Position(10, 22)
        );
        Blockage blockage = new Blockage(startTime, endTime, blockagePoints);
        setId(blockage, 1L);
        
        when(blockageService.findById(1L)).thenReturn(Optional.of(blockage));

        // When & Then
        mockMvc.perform(get("/api/blockages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.startTime").isNotEmpty())
                .andExpect(jsonPath("$.endTime").isNotEmpty());
    }

    @Test
    public void testGetBlockageByIdNotFound() throws Exception {
        // Given
        when(blockageService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/blockages/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetActiveBlockages() throws Exception {
        // Given
        LocalDateTime activeTime = LocalDateTime.of(2025, 5, 1, 10, 0);
        
        LocalDateTime startTime = LocalDateTime.of(2025, 5, 1, 8, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 5, 1, 12, 0);
        List<Position> blockagePoints = Arrays.asList(
                new Position(10, 20),
                new Position(10, 21)
        );
        Blockage blockage = new Blockage(startTime, endTime, blockagePoints);
        setId(blockage, 1L);
        
        when(blockageService.findByActiveAtDateTime(activeTime)).thenReturn(Collections.singletonList(blockage));

        // When & Then
        mockMvc.perform(get("/api/blockages")
                .param("activeAt", "2025-05-01T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testGetBlockagesByTimeRange() throws Exception {
        // Given
        LocalDateTime startRange = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime endRange = LocalDateTime.of(2025, 5, 31, 23, 59);
        
        LocalDateTime startTime = LocalDateTime.of(2025, 5, 15, 8, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 5, 15, 12, 0);
        List<Position> blockagePoints = Arrays.asList(
                new Position(10, 20),
                new Position(10, 21)
        );
        Blockage blockage = new Blockage(startTime, endTime, blockagePoints);
        setId(blockage, 1L);
        
        when(blockageService.findByTimeRange(startRange, endRange)).thenReturn(Collections.singletonList(blockage));

        // When & Then
        mockMvc.perform(get("/api/blockages")
                .param("startTime", "2025-05-01T00:00:00")
                .param("endTime", "2025-05-31T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testCreateBlockage() throws Exception {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2025, 5, 15, 8, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 5, 15, 12, 0);
        List<Position> blockagePoints = Arrays.asList(
                new Position(10, 20),
                new Position(10, 21),
                new Position(10, 22)
        );
        Blockage blockage = new Blockage(startTime, endTime, blockagePoints);
        
        Blockage savedBlockage = new Blockage(startTime, endTime, blockagePoints);
        setId(savedBlockage, 1L);
        
        when(blockageService.save(any(Blockage.class))).thenReturn(savedBlockage);

        // When & Then
        mockMvc.perform(post("/api/blockages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blockage)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testUpdateBlockage() throws Exception {
        // Given
        LocalDateTime originalStart = LocalDateTime.of(2025, 5, 15, 8, 0);
        LocalDateTime originalEnd = LocalDateTime.of(2025, 5, 15, 12, 0);
        List<Position> originalPoints = Arrays.asList(
                new Position(10, 20),
                new Position(10, 21)
        );
        Blockage originalBlockage = new Blockage(originalStart, originalEnd, originalPoints);
        setId(originalBlockage, 1L);
        
        // Updated blockage has different time
        LocalDateTime updatedStart = LocalDateTime.of(2025, 5, 15, 9, 0);
        LocalDateTime updatedEnd = LocalDateTime.of(2025, 5, 15, 13, 0);
        Blockage updatedBlockage = new Blockage(updatedStart, updatedEnd, originalPoints);
        setId(updatedBlockage, 1L);
        
        when(blockageService.findById(1L)).thenReturn(Optional.of(originalBlockage));
        when(blockageService.save(any(Blockage.class))).thenReturn(updatedBlockage);

        // When & Then
        mockMvc.perform(put("/api/blockages/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedBlockage)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("2025-05-15T09:00:00"))
                .andExpect(jsonPath("$.endTime").value("2025-05-15T13:00:00"));
    }

    @Test
    public void testUpdateBlockageNotFound() throws Exception {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2025, 5, 15, 8, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 5, 15, 12, 0);
        List<Position> blockagePoints = Arrays.asList(
                new Position(10, 20),
                new Position(10, 21)
        );
        Blockage blockage = new Blockage(startTime, endTime, blockagePoints);
        setId(blockage, 999L);
        
        when(blockageService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/blockages/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blockage)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteBlockage() throws Exception {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2025, 5, 15, 8, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 5, 15, 12, 0);
        List<Position> blockagePoints = Arrays.asList(
                new Position(10, 20),
                new Position(10, 21)
        );
        Blockage blockage = new Blockage(startTime, endTime, blockagePoints);
        setId(blockage, 1L);
        
        when(blockageService.findById(1L)).thenReturn(Optional.of(blockage));
        doNothing().when(blockageService).deleteById(1L);

        // When & Then
        mockMvc.perform(delete("/api/blockages/1"))
                .andExpect(status().isNoContent());
        
        verify(blockageService, times(1)).deleteById(1L);
    }

    @Test
    public void testDeleteBlockageNotFound() throws Exception {
        // Given
        when(blockageService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/blockages/999"))
                .andExpect(status().isNotFound());
        
        verify(blockageService, never()).deleteById(999L);
    }
} 