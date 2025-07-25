package com.example.plgsystem.controller;

import com.example.plgsystem.dto.BlockageDTO;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.service.BlockageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private Blockage blockage1;
    private List<Blockage> blockages;
    private Page<Blockage> blockagePage;
    private UUID blockageId1;
    private UUID blockageId2;

    @BeforeEach
    public void setUp() {
        // Crear ID fijos para pruebas
        blockageId1 = UUID.fromString("a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6");
        blockageId2 = UUID.fromString("b2c3d4e5-f6a7-48b9-c0d1-e2f3a4b5c6d7");

        // Crear bloqueos para pruebas
        LocalDateTime startTime1 = LocalDateTime.of(2025, 5, 1, 8, 0);
        LocalDateTime endTime1 = LocalDateTime.of(2025, 5, 1, 12, 0);
        List<Position> blockagePoints1 = Arrays.asList(
                new Position(10, 20),
                new Position(10, 21),
                new Position(10, 22));
        blockage1 = new Blockage(startTime1, endTime1, blockagePoints1);
        // Establecer ID manualmente
        try {
            java.lang.reflect.Field idField = Blockage.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(blockage1, blockageId1);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }

        LocalDateTime startTime2 = LocalDateTime.of(2025, 5, 2, 14, 0);
        LocalDateTime endTime2 = LocalDateTime.of(2025, 5, 2, 18, 0);
        List<Position> blockagePoints2 = Arrays.asList(
                new Position(30, 40),
                new Position(31, 40),
                new Position(32, 40));
        Blockage blockage2 = new Blockage(startTime2, endTime2, blockagePoints2);
        // Establecer ID manualmente
        try {
            java.lang.reflect.Field idField = Blockage.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(blockage2, blockageId2);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }

        // Lista de bloqueos
        blockages = Arrays.asList(blockage1, blockage2);

        // Página de bloqueos para pruebas paginadas
        blockagePage = new PageImpl<>(blockages, PageRequest.of(0, 10), 2);
    }

    @Test
    public void testGetAllBlockages_WithPagination() throws Exception {
        // Given
        when(blockageService.findAllPaged(any(Pageable.class))).thenReturn(blockagePage);

        // When & Then
        mockMvc.perform(get("/api/blockages")
                .param("paginated", "true")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(blockageId1.toString()))
                .andExpect(jsonPath("$.content[1].id").value(blockageId2.toString()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testGetBlockageById() throws Exception {
        // Given
        when(blockageService.findById(blockageId1)).thenReturn(Optional.of(blockage1));

        // When & Then
        mockMvc.perform(get("/api/blockages/" + blockageId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(blockageId1.toString()))
                .andExpect(jsonPath("$.startTime").isNotEmpty())
                .andExpect(jsonPath("$.endTime").isNotEmpty());
    }

    @Test
    public void testGetBlockageByIdNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(blockageService.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/blockages/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetActiveBlockages_WithPagination() throws Exception {
        // Given
        LocalDateTime activeTime = LocalDateTime.of(2025, 5, 1, 10, 0);
        Page<Blockage> activeBlockagePage = new PageImpl<>(Collections.singletonList(blockage1),
                PageRequest.of(0, 10), 1);

        when(blockageService.findByActiveAtDateTimePaged(eq(activeTime), any(Pageable.class)))
                .thenReturn(activeBlockagePage);

        // When & Then
        mockMvc.perform(get("/api/blockages")
                .param("paginated", "true")
                .param("activeAt", "2025-05-01T10:00:00")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(blockageId1.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    public void testGetBlockagesByTimeRange_WithPagination() throws Exception {
        // Given
        LocalDateTime startRange = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime endRange = LocalDateTime.of(2025, 5, 31, 23, 59);
        Page<Blockage> rangeBlockagePage = new PageImpl<>(blockages, PageRequest.of(0, 10), 2);

        when(blockageService.findByTimeRangePaged(eq(startRange), eq(endRange), any(Pageable.class)))
                .thenReturn(rangeBlockagePage);

        // When & Then
        mockMvc.perform(get("/api/blockages")
                .param("paginated", "true")
                .param("startTime", "2025-05-01T00:00:00")
                .param("endTime", "2025-05-31T23:59:00")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "startTime")
                .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(blockageId1.toString()))
                .andExpect(jsonPath("$.content[1].id").value(blockageId2.toString()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testCreateBlockage() throws Exception {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2025, 5, 15, 8, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 5, 15, 12, 0);
        List<Position> blockagePoints = Arrays.asList(
                new Position(10, 20),
                new Position(10, 21),
                new Position(10, 22));
        BlockageDTO newBlockage = new BlockageDTO(startTime, endTime, blockagePoints);

        // El ID se generará automáticamente al crear el bloqueo
        when(blockageService.save(any(Blockage.class))).thenReturn(newBlockage.toEntity());

        // When & Then
        mockMvc.perform(post("/api/blockages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBlockage)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    public void testCreateBulkBlockages() throws Exception {
        // Given
        LocalDateTime startTime1 = LocalDateTime.of(2025, 6, 1, 8, 0);
        LocalDateTime endTime1 = LocalDateTime.of(2025, 6, 1, 12, 0);
        List<Position> blockagePoints1 = Arrays.asList(
                new Position(10, 20),
                new Position(10, 21));
        BlockageDTO newBlockage1 = new BlockageDTO(startTime1, endTime1, blockagePoints1);
        
        LocalDateTime startTime2 = LocalDateTime.of(2025, 6, 2, 14, 0);
        LocalDateTime endTime2 = LocalDateTime.of(2025, 6, 2, 18, 0);
        List<Position> blockagePoints2 = Arrays.asList(
                new Position(30, 40),
                new Position(31, 40));
        BlockageDTO newBlockage2 = new BlockageDTO(startTime2, endTime2, blockagePoints2);
        
        List<BlockageDTO> newBlockages = Arrays.asList(newBlockage1, newBlockage2);
        
        // Mock service behavior for bulk create
        when(blockageService.save(any(Blockage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When & Then
        mockMvc.perform(post("/api/blockages/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBlockages)))
                .andExpect(status().isCreated());
    }

    @Test
    public void testUpdateBlockage() throws Exception {
        // Given
        // Crear una copia actualizada del bloqueo
        LocalDateTime updatedStart = LocalDateTime.of(2025, 5, 15, 9, 0);
        LocalDateTime updatedEnd = LocalDateTime.of(2025, 5, 15, 13, 0);
        Blockage updatedBlockage = new Blockage(updatedStart, updatedEnd, blockage1.getLines());

        // Establecer ID manualmente
        try {
            java.lang.reflect.Field idField = Blockage.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(updatedBlockage, blockageId1);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }

        when(blockageService.findById(blockageId1)).thenReturn(Optional.of(blockage1));
        when(blockageService.save(any(Blockage.class))).thenReturn(updatedBlockage);

        // When & Then
        mockMvc.perform(put("/api/blockages/" + blockageId1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedBlockage)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(blockageId1.toString()))
                .andExpect(jsonPath("$.startTime").value("2025-05-15T09:00:00"));
    }

    @Test
    public void testUpdateBlockageNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        LocalDateTime startTime = LocalDateTime.of(2025, 5, 15, 8, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 5, 15, 12, 0);
        List<Position> blockagePoints = Arrays.asList(
                new Position(10, 20),
                new Position(10, 21));
        Blockage nonExistentBlockage = new Blockage(startTime, endTime, blockagePoints);

        // Establecer ID manualmente
        try {
            java.lang.reflect.Field idField = Blockage.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(nonExistentBlockage, nonExistentId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }

        when(blockageService.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/blockages/" + nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentBlockage)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteBlockage() throws Exception {
        // Given
        when(blockageService.findById(blockageId1)).thenReturn(Optional.of(blockage1));

        // When & Then
        mockMvc.perform(delete("/api/blockages/" + blockageId1))
                .andExpect(status().isNoContent());

        verify(blockageService, times(1)).save(blockage1);
    }

    @Test
    public void testDeleteBlockageNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(blockageService.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/blockages/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetActiveBlockages_WithoutPagination() throws Exception {
        // Given
        LocalDateTime activeTime = LocalDateTime.of(2025, 5, 1, 10, 0);
        when(blockageService.findByActiveAtDateTime(activeTime))
                .thenReturn(Collections.singletonList(blockage1));

        // When & Then
        mockMvc.perform(get("/api/blockages")
                .param("paginated", "false")
                .param("activeAt", "2025-05-01T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(blockageId1.toString()));
    }

    @Test
    public void testGetAllBlockages_WithoutPagination() throws Exception {
        // Given
        when(blockageService.findAll()).thenReturn(blockages);

        // When & Then
        mockMvc.perform(get("/api/blockages")
                .param("paginated", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(blockageId1.toString()))
                .andExpect(jsonPath("$[1].id").value(blockageId2.toString()));
    }

    @Test
    public void testPaginationParameters() throws Exception {
        // Given
        when(blockageService.findAllPaged(any(Pageable.class))).thenReturn(Page.empty());

        // When
        mockMvc.perform(get("/api/blockages")
                .param("paginated", "true")
                .param("page", "1")
                .param("size", "5")
                .param("sortBy", "startTime")
                .param("direction", "desc"))
                .andExpect(status().isOk());

        // Then
        verify(blockageService).findAllPaged(PageRequest.of(1, 5, Sort.by("startTime").descending()));
    }
}