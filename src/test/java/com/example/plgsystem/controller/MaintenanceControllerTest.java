package com.example.plgsystem.controller;

import com.example.plgsystem.dto.MaintenanceCreateDTO;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.service.MaintenanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaintenanceController.class)
public class MaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaintenanceService maintenanceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllMaintenances() throws Exception {
        // Given
        Maintenance maintenance1 = new Maintenance("V001", LocalDate.now());
        Maintenance maintenance2 = new Maintenance("V002", LocalDate.now().plusDays(1));
        
        when(maintenanceService.findAll()).thenReturn(Arrays.asList(maintenance1, maintenance2));

        // When & Then
        mockMvc.perform(get("/api/maintenances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V002"));
    }

    @Test
    public void testGetMaintenancesByVehicleId() throws Exception {
        // Given
        Maintenance maintenance1 = new Maintenance("V001", LocalDate.now());
        Maintenance maintenance2 = new Maintenance("V001", LocalDate.now().plusDays(1));
        
        when(maintenanceService.findByVehicleId("V001")).thenReturn(Arrays.asList(maintenance1, maintenance2));

        // When & Then
        mockMvc.perform(get("/api/maintenances").param("vehicleId", "V001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V001"));
    }

    @Test
    public void testGetMaintenancesByDate() throws Exception {
        // Given
        LocalDate date = LocalDate.of(2025, 5, 15);
        Maintenance maintenance1 = new Maintenance("V001", date);
        Maintenance maintenance2 = new Maintenance("V002", date);
        
        when(maintenanceService.findByDate(date)).thenReturn(Arrays.asList(maintenance1, maintenance2));

        // When & Then
        mockMvc.perform(get("/api/maintenances").param("date", "2025-05-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V002"));
    }

    @Test
    public void testGetMaintenancesByVehicleIdAndDate() throws Exception {
        // Given
        LocalDate date = LocalDate.of(2025, 5, 15);
        Maintenance maintenance = new Maintenance("V001", date);
        
        when(maintenanceService.findByVehicleIdAndDate("V001", date)).thenReturn(Collections.singletonList(maintenance));

        // When & Then
        mockMvc.perform(get("/api/maintenances")
                .param("vehicleId", "V001")
                .param("date", "2025-05-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"));
    }

    @Test
    public void testGetMaintenancesByDateRange() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2025, 5, 1);
        LocalDate endDate = LocalDate.of(2025, 5, 31);
        Maintenance maintenance1 = new Maintenance("V001", startDate);
        Maintenance maintenance2 = new Maintenance("V002", endDate);
        
        when(maintenanceService.findByDateRange(startDate, endDate)).thenReturn(Arrays.asList(maintenance1, maintenance2));

        // When & Then
        mockMvc.perform(get("/api/maintenances")
                .param("startDate", "2025-05-01")
                .param("endDate", "2025-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V002"));
    }

    @Test
    public void testGetActiveMaintenances() throws Exception {
        // Given
        Maintenance maintenance1 = new Maintenance("V001", LocalDate.now());
        maintenance1.setRealStart(LocalDateTime.now().minusHours(2));
        
        Maintenance maintenance2 = new Maintenance("V002", LocalDate.now());
        maintenance2.setRealStart(LocalDateTime.now().minusHours(1));
        
        when(maintenanceService.findActiveMaintenances()).thenReturn(Arrays.asList(maintenance1, maintenance2));

        // When & Then
        mockMvc.perform(get("/api/maintenances/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V002"));
    }

    @Test
    public void testCreateMaintenance() throws Exception {
        // Given
        MaintenanceCreateDTO createDTO = new MaintenanceCreateDTO();
        createDTO.setVehicleId("V001");
        createDTO.setAssignedDate(LocalDate.of(2025, 5, 15));
        
        Maintenance maintenance = new Maintenance("V001", LocalDate.of(2025, 5, 15));
        
        when(maintenanceService.createMaintenance(eq("V001"), any(LocalDate.class))).thenReturn(maintenance);

        // When & Then
        mockMvc.perform(post("/api/maintenances")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleId").value("V001"));
    }
} 