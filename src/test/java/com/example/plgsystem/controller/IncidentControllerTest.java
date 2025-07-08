package com.example.plgsystem.controller;

import com.example.plgsystem.dto.IncidentCreateDTO;
import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.service.IncidentService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IncidentController.class)
public class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentService incidentService;

    @MockitoBean
    private VehicleService vehicleService;

    @Autowired
    private ObjectMapper objectMapper;

    private Vehicle vehicle1;
    private Vehicle vehicle2;
    private Incident incident1;
    private Incident incident2;
    private UUID incidentId1;
    private UUID incidentId2;
    private LocalDateTime occurrenceTime1;
    private LocalDateTime occurrenceTime2;

    @BeforeEach
    public void setUp() {
        // Crear IDs fijos para pruebas
        incidentId1 = UUID.fromString("a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6");
        incidentId2 = UUID.fromString("b2c3d4e5-f6a7-48b9-c0d1-e2f3a4b5c6d7");

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

        // Crear tiempos de ocurrencia
        occurrenceTime1 = LocalDateTime.of(2025, 5, 15, 9, 0); // 9 AM - Shift T1
        occurrenceTime2 = LocalDateTime.of(2025, 5, 15, 14, 0); // 2 PM - Shift T2

        // Crear incidentes para pruebas
        incident1 = new Incident(vehicle1, IncidentType.TI1, occurrenceTime1);
        incident2 = new Incident(vehicle2, IncidentType.TI2, occurrenceTime2);

        // Establecer IDs manualmente
        try {
            java.lang.reflect.Field idField = Incident.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(incident1, incidentId1);
            idField.set(incident2, incidentId2);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }

        // Configurar ubicaciones
        incident1.setLocation(new Position(15, 25));
        incident2.setLocation(new Position(35, 45));
    }

    @Test
    public void testGetAllIncidents() throws Exception {
        // Given
        when(incidentService.findAll()).thenReturn(Arrays.asList(incident1, incident2));

        // When & Then
        mockMvc.perform(get("/api/incidents")
                .param("paginated", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V-002"));
    }

    @Test
    public void testGetAllIncidents_WithPagination() throws Exception {
        // Given
        Page<Incident> incidentPage = new PageImpl<>(Arrays.asList(incident1, incident2),
                PageRequest.of(0, 10), 2);

        when(incidentService.findAllPaged(any(Pageable.class))).thenReturn(incidentPage);

        // When & Then
        mockMvc.perform(get("/api/incidents")
                .param("paginated", "true")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$.content[1].vehicleId").value("V-002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testGetIncidentById() throws Exception {
        // Given
        when(incidentService.findById(incidentId1)).thenReturn(Optional.of(incident1));

        // When & Then
        mockMvc.perform(get("/api/incidents/" + incidentId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidentId1.toString()))
                .andExpect(jsonPath("$.vehicleId").value("V-001"));
    }

    @Test
    public void testGetIncidentByIdNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(incidentService.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/incidents/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetIncidentsByVehicleId() throws Exception {
        // Given
        when(incidentService.findByVehicleId("V-001")).thenReturn(Arrays.asList(incident1));

        // When & Then
        mockMvc.perform(get("/api/incidents")
                .param("paginated", "false")
                .param("vehicleId", "V-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V-001"));
    }

    @Test
    public void testGetIncidentsByDateRange() throws Exception {
        // Given
        LocalDateTime start = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 5, 31, 23, 59);

        when(incidentService.findByDateRange(start, end)).thenReturn(Arrays.asList(incident1, incident2));

        // When & Then
        mockMvc.perform(get("/api/incidents")
                .param("paginated", "false")
                .param("startDate", "2025-05-01T00:00:00")
                .param("endDate", "2025-05-31T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V-002"));
    }

    @Test
    public void testGetIncidentsByVehicleIdAndDateRange() throws Exception {
        // Given
        String vehicleId = "V-001";
        LocalDateTime start = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 5, 31, 23, 59);

        when(incidentService.findByVehicleAndDateRange(vehicleId, start, end))
                .thenReturn(Collections.singletonList(incident1));

        // When & Then
        mockMvc.perform(get("/api/incidents")
                .param("paginated", "false")
                .param("vehicleId", vehicleId)
                .param("startDate", "2025-05-01T00:00:00")
                .param("endDate", "2025-05-31T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value(vehicleId));
    }

    @Test
    public void testCreateIncident() throws Exception {
        // Given
        IncidentCreateDTO createDTO = new IncidentCreateDTO();
        createDTO.setVehicleId("V-001");
        createDTO.setType(IncidentType.TI1);
        createDTO.setOccurrenceTime(LocalDateTime.of(2025, 5, 15, 10, 0));
        createDTO.setLocation(new Position(10, 20));

        when(vehicleService.findById("V-001")).thenReturn(Optional.of(vehicle1));
        when(incidentService.save(any(Incident.class))).thenReturn(incident1);

        // When & Then
        mockMvc.perform(post("/api/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(incidentId1.toString()))
                .andExpect(jsonPath("$.vehicleId").value("V-001"));
    }

    @Test
    public void testCreateIncidentWithInvalidVehicle() throws Exception {
        // Given
        IncidentCreateDTO createDTO = new IncidentCreateDTO();
        createDTO.setVehicleId("nonexistent");
        createDTO.setType(IncidentType.TI1);
        createDTO.setOccurrenceTime(LocalDateTime.now());

        when(vehicleService.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testResolveIncident() throws Exception {
        // Given
        Incident resolvedIncident = new Incident(vehicle1, IncidentType.TI1, occurrenceTime1);

        // Establecer ID manualmente
        try {
            java.lang.reflect.Field idField = Incident.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(resolvedIncident, incidentId1);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }

        resolvedIncident.setResolved(true);

        when(incidentService.resolveIncident(incidentId1)).thenReturn(Optional.of(resolvedIncident));

        // When & Then
        mockMvc.perform(patch("/api/incidents/" + incidentId1 + "/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidentId1.toString()))
                .andExpect(jsonPath("$.resolved").value(true));
    }

    @Test
    public void testResolveIncidentNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(incidentService.resolveIncident(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(patch("/api/incidents/" + nonExistentId + "/resolve"))
                .andExpect(status().isNotFound());
    }
}