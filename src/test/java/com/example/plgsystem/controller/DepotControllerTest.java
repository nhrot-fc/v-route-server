package com.example.plgsystem.controller;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.service.DepotService;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepotController.class)
public class DepotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepotService depotService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllDepots_WithPagination() throws Exception {
        // Given
        Depot depot1 = new Depot("D001", new Position(10, 20), 1000, true);
        Depot depot2 = new Depot("D002", new Position(30, 40), 2000, false);

        List<Depot> depots = Arrays.asList(depot1, depot2);
        Page<Depot> depotPage = new PageImpl<>(depots, PageRequest.of(0, 10), 2);
        
        when(depotService.findAllPaged(any(Pageable.class))).thenReturn(depotPage);

        // When & Then
        mockMvc.perform(get("/api/depots")
                .param("paginated", "true")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("D001"))
                .andExpect(jsonPath("$.content[1].id").value("D002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testGetDepotByCanRefuel_WithPagination() throws Exception {
        // Given
        Depot depot1 = new Depot("D001", new Position(10, 20), 1000, true);
        Depot depot2 = new Depot("D002", new Position(30, 40), 2000, true);

        List<Depot> depots = Arrays.asList(depot1, depot2);
        Page<Depot> depotPage = new PageImpl<>(depots, PageRequest.of(0, 10), 2);
        
        when(depotService.findByCanRefuelPaged(eq(true), any(Pageable.class))).thenReturn(depotPage);

        // When & Then
        mockMvc.perform(get("/api/depots")
                .param("paginated", "true")
                .param("canRefuel", "true")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("D001"))
                .andExpect(jsonPath("$.content[1].id").value("D002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testGetDepotById() throws Exception {
        // Given
        Depot depot = new Depot("D001", new Position(10, 20), 1000, true);

        when(depotService.findById("D001")).thenReturn(Optional.of(depot));

        // When & Then
        mockMvc.perform(get("/api/depots/D001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("D001"))
                .andExpect(jsonPath("$.glpCapacityM3").value(1000))
                .andExpect(jsonPath("$.canRefuel").value(true));
    }

    @Test
    public void testGetDepotByIdNotFound() throws Exception {
        // Given
        when(depotService.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/depots/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateDepot() throws Exception {
        // Given
        Depot depot = new Depot("D001", new Position(10, 20), 1000, true);
        String depotJson = objectMapper.writeValueAsString(depot);

        when(depotService.save(any(Depot.class))).thenReturn(depot);

        // When & Then
        mockMvc.perform(post("/api/depots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(depotJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("D001"));
    }

    @Test
    public void testUpdateDepot() throws Exception {
        // Given
        Depot originalDepot = new Depot("D001", new Position(10, 20), 1000, true);
        originalDepot.setCurrentGlpM3(500);

        // Updated depot has more GLP
        Depot updatedDepot = new Depot("D001", new Position(10, 20), 1000, true);
        updatedDepot.setCurrentGlpM3(800);

        String updatedDepotJson = objectMapper.writeValueAsString(updatedDepot);

        when(depotService.findById("D001")).thenReturn(Optional.of(originalDepot));
        when(depotService.save(any(Depot.class))).thenReturn(updatedDepot);

        // When & Then
        mockMvc.perform(put("/api/depots/D001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedDepotJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGlpM3").value(800));
    }

    @Test
    public void testUpdateDepotNotFound() throws Exception {
        // Given
        Depot depot = new Depot("nonexistent", new Position(10, 20), 1000, true);
        String depotJson = objectMapper.writeValueAsString(depot);

        when(depotService.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/depots/nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(depotJson))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteDepot() throws Exception {
        // Given
        when(depotService.findById("D001")).thenReturn(Optional.of(new Depot("D001", new Position(10, 20), 1000, true)));
        doNothing().when(depotService).deleteById("D001");

        // When & Then
        mockMvc.perform(delete("/api/depots/D001"))
                .andExpect(status().isNoContent());

        verify(depotService, times(1)).deleteById("D001");
    }
    
    @Test
    public void testPaginationAndSorting() throws Exception {
        // Given
        Depot depot = new Depot("D001", new Position(10, 20), 1000, true);
        depot.setCurrentGlpM3(800);

        List<Depot> depots = Arrays.asList(depot);
        Page<Depot> depotPage = new PageImpl<>(depots, PageRequest.of(1, 5), 12);
        
        when(depotService.findByMinCurrentGlpPaged(eq(500), any(Pageable.class))).thenReturn(depotPage);

        // When & Then
        mockMvc.perform(get("/api/depots")
                .param("paginated", "true")
                .param("minCurrentGlp", "500")
                .param("page", "1")
                .param("size", "5")
                .param("sortBy", "currentGlpM3")
                .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("D001"))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }
    
    @Test
    public void testGetAllDepots_WithoutPagination() throws Exception {
        // Given
        Depot depot1 = new Depot("D001", new Position(10, 20), 1000, true);
        Depot depot2 = new Depot("D002", new Position(30, 40), 2000, false);

        List<Depot> depots = Arrays.asList(depot1, depot2);
        
        when(depotService.findAll()).thenReturn(depots);

        // When & Then
        mockMvc.perform(get("/api/depots")
                .param("paginated", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("D001"))
                .andExpect(jsonPath("$[1].id").value("D002"));
    }
}
