package com.example.plgsystem.controller;

import com.example.plgsystem.dto.MaintenanceCreateDTO;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.service.MaintenanceService;
import com.example.plgsystem.service.VehicleService;
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

import java.time.LocalDate;
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

@WebMvcTest(MaintenanceController.class)
public class MaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaintenanceService maintenanceService;
    
    @MockitoBean
    private VehicleService vehicleService;

    @Autowired
    private ObjectMapper objectMapper;
    
    private Vehicle vehicle1;
    private Vehicle vehicle2;
    private Maintenance maintenance1;
    private Maintenance maintenance2;
    private UUID maintenanceId1;
    private UUID maintenanceId2;
    private LocalDate assignedDate1;
    private LocalDate assignedDate2;

    @BeforeEach
    public void setUp() {
        // Crear IDs fijos para pruebas
        maintenanceId1 = UUID.fromString("a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6");
        maintenanceId2 = UUID.fromString("b2c3d4e5-f6a7-48b9-c0d1-e2f3a4b5c6d7");
        
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
        
        // Crear fechas para mantenimientos
        assignedDate1 = LocalDate.now();
        assignedDate2 = LocalDate.now().plusDays(1);
        
        // Crear mantenimientos para pruebas
        maintenance1 = new Maintenance(vehicle1, assignedDate1);
        maintenance2 = new Maintenance(vehicle2, assignedDate2);
        
        // Establecer IDs manualmente
        try {
            java.lang.reflect.Field idField = Maintenance.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(maintenance1, maintenanceId1);
            idField.set(maintenance2, maintenanceId2);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }
        
        // Configurar tiempos de inicio y fin para algunos tests
        maintenance1.setRealStart(LocalDateTime.now().minusHours(2));
        maintenance2.setRealStart(LocalDateTime.now().minusHours(1));
    }

    @Test
    public void testGetAllMaintenances() throws Exception {
        // Given
        when(maintenanceService.findAll()).thenReturn(Arrays.asList(maintenance1, maintenance2));

        // When & Then
        mockMvc.perform(get("/api/maintenances")
                .param("paginated", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V-002"));
    }
    
    @Test
    public void testGetAllMaintenances_WithPagination() throws Exception {
        // Given
        Page<Maintenance> maintenancePage = new PageImpl<>(
                Arrays.asList(maintenance1, maintenance2),
                PageRequest.of(0, 10),
                2
        );
        
        when(maintenanceService.findAllPaged(any(Pageable.class))).thenReturn(maintenancePage);

        // When & Then
        mockMvc.perform(get("/api/maintenances")
                .param("paginated", "true")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$.content[1].vehicleId").value("V-002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testGetMaintenancesByVehicleId() throws Exception {
        // Given
        when(maintenanceService.findByVehicleId("V-001")).thenReturn(Collections.singletonList(maintenance1));

        // When & Then
        mockMvc.perform(get("/api/maintenances")
                .param("paginated", "false")
                .param("vehicleId", "V-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V-001"));
    }

    @Test
    public void testGetMaintenancesByDate() throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2025, 5, 15);
        Maintenance dateMaintenance1 = new Maintenance(vehicle1, testDate);
        Maintenance dateMaintenance2 = new Maintenance(vehicle2, testDate);
        
        when(maintenanceService.findByDate(testDate)).thenReturn(Arrays.asList(dateMaintenance1, dateMaintenance2));

        // When & Then
        mockMvc.perform(get("/api/maintenances")
                .param("paginated", "false")
                .param("date", "2025-05-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V-002"));
    }

    @Test
    public void testGetMaintenancesByVehicleIdAndDate() throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2025, 5, 15);
        Maintenance dateMaintenance = new Maintenance(vehicle1, testDate);
        
        when(maintenanceService.findByVehicleIdAndDate("V-001", testDate)).thenReturn(Collections.singletonList(dateMaintenance));

        // When & Then
        mockMvc.perform(get("/api/maintenances")
                .param("paginated", "false")
                .param("vehicleId", "V-001")
                .param("date", "2025-05-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V-001"));
    }

    @Test
    public void testGetMaintenancesByDateRange() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2025, 5, 1);
        LocalDate endDate = LocalDate.of(2025, 5, 31);
        Maintenance rangeMaintenance1 = new Maintenance(vehicle1, startDate);
        Maintenance rangeMaintenance2 = new Maintenance(vehicle2, endDate);
        
        when(maintenanceService.findByDateRange(startDate, endDate)).thenReturn(Arrays.asList(rangeMaintenance1, rangeMaintenance2));

        // When & Then
        mockMvc.perform(get("/api/maintenances")
                .param("paginated", "false")
                .param("startDate", "2025-05-01")
                .param("endDate", "2025-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V-002"));
    }

    @Test
    public void testGetActiveMaintenances() throws Exception {
        // Given
        when(maintenanceService.findActiveMaintenances()).thenReturn(Arrays.asList(maintenance1, maintenance2));

        // When & Then
        mockMvc.perform(get("/api/maintenances/active")
                .param("paginated", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$[1].vehicleId").value("V-002"));
    }
    
    @Test
    public void testGetActiveMaintenances_WithPagination() throws Exception {
        // Given
        Page<Maintenance> maintenancePage = new PageImpl<>(
                Arrays.asList(maintenance1, maintenance2),
                PageRequest.of(0, 10),
                2
        );
        
        when(maintenanceService.findActiveMaintenancesPaged(any(Pageable.class))).thenReturn(maintenancePage);

        // When & Then
        mockMvc.perform(get("/api/maintenances/active")
                .param("paginated", "true")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vehicleId").value("V-001"))
                .andExpect(jsonPath("$.content[1].vehicleId").value("V-002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testCreateMaintenance() throws Exception {
        // Given
        MaintenanceCreateDTO createDTO = new MaintenanceCreateDTO();
        createDTO.setVehicleId("V-001");
        createDTO.setAssignedDate(LocalDate.of(2025, 5, 15));
        
        when(maintenanceService.createMaintenance(eq("V-001"), any(LocalDate.class))).thenReturn(Optional.of(maintenance1));

        // When & Then
        mockMvc.perform(post("/api/maintenances")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleId").value("V-001"));
    }
    
    @Test
    public void testGetMaintenanceById() throws Exception {
        // Given
        when(maintenanceService.findById(maintenanceId1)).thenReturn(Optional.of(maintenance1));

        // When & Then
        mockMvc.perform(get("/api/maintenances/" + maintenanceId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value("V-001"));
    }
    
    @Test
    public void testGetMaintenanceByIdNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(maintenanceService.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/maintenances/" + nonExistentId))
                .andExpect(status().isNotFound());
    }
} 