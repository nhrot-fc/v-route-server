package com.example.plgsystem.controller;

import com.example.plgsystem.dto.IncidentCreateDTO;
import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.service.IncidentService;
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

    @Autowired
    private ObjectMapper objectMapper;
    
    // Utility method to set ID using reflection
    private void setId(Incident incident, Long id) {
        try {
            Field idField = Incident.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(incident, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }
    }

    @Test
    public void testGetAllIncidents() throws Exception {
        // Given
        Incident incident1 = new Incident("V001", IncidentType.TYPE_1, Shift.T1);
        setId(incident1, 1L);
        incident1.setOccurrenceTime(LocalDateTime.now().minusHours(2));
        
        Incident incident2 = new Incident("V002", IncidentType.TYPE_2, Shift.T2);
        setId(incident2, 2L);
        incident2.setOccurrenceTime(LocalDateTime.now().minusHours(1));
        
        when(incidentService.findAll()).thenReturn(Arrays.asList(incident1, incident2));

        // When & Then
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V002"));
    }

    @Test
    public void testGetIncidentById() throws Exception {
        // Given
        Incident incident = new Incident("V001", IncidentType.TYPE_1, Shift.T1);
        setId(incident, 1L);
        incident.setOccurrenceTime(LocalDateTime.now());
        
        when(incidentService.findById(1L)).thenReturn(Optional.of(incident));

        // When & Then
        mockMvc.perform(get("/api/incidents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vehicleId").value("V001"));
    }

    @Test
    public void testGetIncidentByIdNotFound() throws Exception {
        // Given
        when(incidentService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/incidents/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetIncidentsByVehicleId() throws Exception {
        // Given
        Incident incident1 = new Incident("V001", IncidentType.TYPE_1, Shift.T1);
        setId(incident1, 1L);
        incident1.setOccurrenceTime(LocalDateTime.now().minusDays(1));
        
        Incident incident2 = new Incident("V001", IncidentType.TYPE_2, Shift.T2);
        setId(incident2, 2L);
        incident2.setOccurrenceTime(LocalDateTime.now());
        
        when(incidentService.findByVehicleId("V001")).thenReturn(Arrays.asList(incident1, incident2));

        // When & Then
        mockMvc.perform(get("/api/incidents").param("vehicleId", "V001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V001"));
    }

    @Test
    public void testGetIncidentsByDateRange() throws Exception {
        // Given
        LocalDateTime start = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 5, 31, 23, 59);
        
        Incident incident1 = new Incident("V001", IncidentType.TYPE_1, Shift.T1);
        setId(incident1, 1L);
        incident1.setOccurrenceTime(LocalDateTime.of(2025, 5, 15, 10, 0));
        
        Incident incident2 = new Incident("V002", IncidentType.TYPE_2, Shift.T2);
        setId(incident2, 2L);
        incident2.setOccurrenceTime(LocalDateTime.of(2025, 5, 20, 14, 30));
        
        when(incidentService.findByDateRange(start, end)).thenReturn(Arrays.asList(incident1, incident2));

        // When & Then
        mockMvc.perform(get("/api/incidents")
                .param("startDate", "2025-05-01T00:00:00")
                .param("endDate", "2025-05-31T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V002"));
    }

    @Test
    public void testGetIncidentsByVehicleIdAndDateRange() throws Exception {
        // Given
        String vehicleId = "V001";
        LocalDateTime start = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 5, 31, 23, 59);
        
        Incident incident1 = new Incident(vehicleId, IncidentType.TYPE_1, Shift.T1);
        setId(incident1, 1L);
        incident1.setOccurrenceTime(LocalDateTime.of(2025, 5, 15, 10, 0));
        
        when(incidentService.findByVehicleAndDateRange(vehicleId, start, end))
                .thenReturn(Collections.singletonList(incident1));

        // When & Then
        mockMvc.perform(get("/api/incidents")
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
        createDTO.setVehicleId("V001");
        createDTO.setType(IncidentType.TYPE_1);
        createDTO.setShift(Shift.T1);
        createDTO.setOccurrenceTime(LocalDateTime.of(2025, 5, 15, 10, 0));
        createDTO.setLocation(new Position(10, 20));
        createDTO.setTransferableGlp(50.5);
        
        Incident savedIncident = new Incident("V001", IncidentType.TYPE_1, Shift.T1);
        setId(savedIncident, 1L);
        savedIncident.setOccurrenceTime(LocalDateTime.of(2025, 5, 15, 10, 0));
        savedIncident.setLocation(new Position(10, 20));
        savedIncident.setTransferableGlp(50.5);
        
        when(incidentService.save(any(Incident.class))).thenReturn(savedIncident);

        // When & Then
        mockMvc.perform(post("/api/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vehicleId").value("V001"))
                .andExpect(jsonPath("$.transferableGlp").value(50.5));
    }

    @Test
    public void testResolveIncident() throws Exception {
        // Given
        Incident incident = new Incident("V001", IncidentType.TYPE_1, Shift.T1);
        setId(incident, 1L);
        incident.setOccurrenceTime(LocalDateTime.now());
        incident.setResolved(false);
        
        Incident resolvedIncident = new Incident("V001", IncidentType.TYPE_1, Shift.T1);
        setId(resolvedIncident, 1L);
        resolvedIncident.setOccurrenceTime(LocalDateTime.now());
        resolvedIncident.setResolved(true);
        
        when(incidentService.resolveIncident(1L)).thenReturn(Optional.of(resolvedIncident));

        // When & Then
        mockMvc.perform(patch("/api/incidents/1/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolved").value(true));
    }

    @Test
    public void testResolveIncidentNotFound() throws Exception {
        // Given
        when(incidentService.resolveIncident(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(patch("/api/incidents/999/resolve"))
                .andExpect(status().isNotFound());
    }
} 