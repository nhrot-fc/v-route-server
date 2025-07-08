package com.example.plgsystem.controller;

import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.service.DepotService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
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

    private Depot mainDepot;
    private Depot auxiliaryDepot;
    private Page<Depot> depotPage;

    @BeforeEach
    public void setUp() {
        // Crear depósitos para pruebas
        mainDepot = new Depot("MAIN_PLANT", new Position(10, 20), 5000, DepotType.MAIN);
        auxiliaryDepot = new Depot("AUX_DEPOT", new Position(30, 40), 2000, DepotType.AUXILIARY);

        // Configurar algunos valores iniciales
        mainDepot.setCurrentGlpM3(4000);
        auxiliaryDepot.setCurrentGlpM3(1500);

        // Lista de depósitos
        List<Depot> depots = Arrays.asList(mainDepot, auxiliaryDepot);

        // Página de depósitos para pruebas paginadas
        depotPage = new PageImpl<>(depots, PageRequest.of(0, 10), 2);
    }

    @Test
    public void testGetAllDepots_WithPagination() throws Exception {
        // Given
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
                .andExpect(jsonPath("$.content[0].id").value("MAIN_PLANT"))
                .andExpect(jsonPath("$.content[1].id").value("AUX_DEPOT"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testGetDepotById() throws Exception {
        // Given
        when(depotService.findById("MAIN_PLANT")).thenReturn(Optional.of(mainDepot));

        // When & Then
        mockMvc.perform(get("/api/depots/MAIN_PLANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("MAIN_PLANT"))
                .andExpect(jsonPath("$.glpCapacityM3").value(5000))
                .andExpect(jsonPath("$.type").value("MAIN"))
                .andExpect(jsonPath("$.currentGlpM3").value(4000));
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
        Depot newDepot = new Depot("NEW_DEPOT", new Position(50, 60), 1000, DepotType.AUXILIARY);
        String depotJson = objectMapper.writeValueAsString(newDepot);

        when(depotService.save(any(Depot.class))).thenReturn(newDepot);

        // When & Then
        mockMvc.perform(post("/api/depots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(depotJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("NEW_DEPOT"))
                .andExpect(jsonPath("$.position.x").value(50))
                .andExpect(jsonPath("$.position.y").value(60))
                .andExpect(jsonPath("$.glpCapacityM3").value(1000))
                .andExpect(jsonPath("$.type").value("AUXILIARY"));
    }

    @Test
    public void testUpdateDepot() throws Exception {
        // Given
        // Crear una copia actualizada del depósito
        Depot updatedDepot = new Depot(auxiliaryDepot.getId(), auxiliaryDepot.getPosition(),
                auxiliaryDepot.getGlpCapacityM3(), auxiliaryDepot.getType());
        updatedDepot.setCurrentGlpM3(1800); // Aumentar el GLP actual

        String updatedDepotJson = objectMapper.writeValueAsString(updatedDepot);

        when(depotService.findById("AUX_DEPOT")).thenReturn(Optional.of(auxiliaryDepot));
        when(depotService.save(any(Depot.class))).thenReturn(updatedDepot);

        // When & Then
        mockMvc.perform(put("/api/depots/AUX_DEPOT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedDepotJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("AUX_DEPOT"))
                .andExpect(jsonPath("$.currentGlpM3").value(1800));
    }

    @Test
    public void testUpdateDepotNotFound() throws Exception {
        // Given
        Depot nonExistentDepot = new Depot("nonexistent", new Position(10, 20), 1000, DepotType.AUXILIARY);
        String depotJson = objectMapper.writeValueAsString(nonExistentDepot);

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
        when(depotService.findById("AUX_DEPOT")).thenReturn(Optional.of(auxiliaryDepot));
        doNothing().when(depotService).deleteById("AUX_DEPOT");

        // When & Then
        mockMvc.perform(delete("/api/depots/AUX_DEPOT"))
                .andExpect(status().isNoContent());

        verify(depotService, times(1)).deleteById("AUX_DEPOT");
    }

    @Test
    public void testGetDepotsByType() throws Exception {
        // Given
        List<Depot> mainDepots = Collections.singletonList(mainDepot);
        when(depotService.findByType(DepotType.MAIN)).thenReturn(mainDepots);

        // When & Then
        mockMvc.perform(get("/api/depots")
                .param("paginated", "false")
                .param("type", "MAIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("MAIN_PLANT"))
                .andExpect(jsonPath("$[0].type").value("MAIN"));
    }

    @Test
    public void testGetDepotsByMinCapacity() throws Exception {
        // Given
        when(depotService.findByMinCapacity(3000)).thenReturn(Collections.singletonList(mainDepot));

        // When & Then
        mockMvc.perform(get("/api/depots")
                .param("paginated", "false")
                .param("minGlpCapacity", "3000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("MAIN_PLANT"))
                .andExpect(jsonPath("$[0].glpCapacityM3").value(5000));
    }

    @Test
    public void testGetMainDepots() throws Exception {
        // Given
        when(depotService.findMainDepots()).thenReturn(Collections.singletonList(mainDepot));

        // When & Then
        mockMvc.perform(get("/api/depots")
                .param("paginated", "false")
                .param("isMain", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("MAIN_PLANT"))
                .andExpect(jsonPath("$[0].type").value("MAIN"));
    }
}
