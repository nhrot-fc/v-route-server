package com.example.plgsystem.controller;

import com.example.plgsystem.model.*;
import com.example.plgsystem.repository.MaintenanceRepository;
import com.example.plgsystem.repository.VehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Vehicle testVehicle;
    private Maintenance testMaintenance;

    @BeforeEach
    public void setup() {
        // Create a test vehicle
        Position position = new Position(10, 10);
        testVehicle = new Vehicle("MAINT-TEST-001", VehicleType.TA, position);
        testVehicle = vehicleRepository.save(testVehicle);
        
        // Create a test maintenance record
        LocalDateTime now = LocalDateTime.now();
        testMaintenance = new Maintenance();
        testMaintenance.setVehicleId(testVehicle.getId());
        testMaintenance.setStartDate(now);
        testMaintenance.setEndDate(now.plusHours(2));
        testMaintenance.setType(MaintenanceType.PREVENTIVE);
        testMaintenance.setDescription("Test maintenance");
        testMaintenance.setCompleted(false);
        testMaintenance = maintenanceRepository.save(testMaintenance);
    }

    @Test
    public void testGetAllMaintenance() throws Exception {
        mockMvc.perform(get("/api/maintenance"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].vehicleId", is(testVehicle.getId())))
                .andExpect(jsonPath("$[0].type", is(testMaintenance.getType().toString())))
                .andExpect(jsonPath("$[0].description", is(testMaintenance.getDescription())));
    }

    @Test
    public void testGetMaintenanceById() throws Exception {
        mockMvc.perform(get("/api/maintenance/{id}", testMaintenance.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testMaintenance.getId().intValue())))
                .andExpect(jsonPath("$.vehicleId", is(testVehicle.getId())))
                .andExpect(jsonPath("$.type", is(testMaintenance.getType().toString())));
        
        // Test with non-existent ID
        mockMvc.perform(get("/api/maintenance/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetMaintenanceByVehicle() throws Exception {
        mockMvc.perform(get("/api/maintenance/vehicle/{vehicleId}", testVehicle.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].vehicleId", is(testVehicle.getId())));
        
        // Test with non-existent vehicle ID
        mockMvc.perform(get("/api/maintenance/vehicle/NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testGetMaintenanceByType() throws Exception {
        mockMvc.perform(get("/api/maintenance/type/{type}", MaintenanceType.PREVENTIVE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].type", is(MaintenanceType.PREVENTIVE.toString())));
        
        // Test with a type that doesn't have any records
        mockMvc.perform(get("/api/maintenance/type/{type}", MaintenanceType.CORRECTIVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testGetActiveMaintenance() throws Exception {
        mockMvc.perform(get("/api/maintenance/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testGetActiveMaintenanceForVehicle() throws Exception {
        mockMvc.perform(get("/api/maintenance/active/vehicle/{vehicleId}", testVehicle.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testGetUpcomingMaintenance() throws Exception {
        // Create maintenance scheduled for tomorrow
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        Maintenance upcomingMaintenance = new Maintenance();
        upcomingMaintenance.setVehicleId(testVehicle.getId());
        upcomingMaintenance.setStartDate(tomorrow);
        upcomingMaintenance.setEndDate(tomorrow.plusHours(3));
        upcomingMaintenance.setType(MaintenanceType.PREVENTIVE);
        upcomingMaintenance.setDescription("Upcoming maintenance");
        upcomingMaintenance.setCompleted(false);
        maintenanceRepository.save(upcomingMaintenance);
        
        String startDate = LocalDateTime.now().toString();
        String endDate = LocalDateTime.now().plusDays(2).toString();
        
        mockMvc.perform(get("/api/maintenance/upcoming")
                .param("startDate", startDate)
                .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].description", is("Upcoming maintenance")));
    }

    @Test
    public void testCreateMaintenance() throws Exception {
        // Create a new maintenance record
        LocalDateTime futureDate = LocalDateTime.now().plusDays(3);
        Maintenance newMaintenance = new Maintenance();
        newMaintenance.setVehicleId(testVehicle.getId());
        newMaintenance.setStartDate(futureDate);
        newMaintenance.setEndDate(futureDate.plusHours(4));
        newMaintenance.setType(MaintenanceType.CORRECTIVE);
        newMaintenance.setDescription("New maintenance");
        newMaintenance.setCompleted(false);
        
        mockMvc.perform(post("/api/maintenance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newMaintenance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId", is(testVehicle.getId())))
                .andExpect(jsonPath("$.type", is(MaintenanceType.CORRECTIVE.toString())))
                .andExpect(jsonPath("$.description", is("New maintenance")));
    }

    @Test
    public void testScheduleMaintenance() throws Exception {
        String vehicleId = testVehicle.getId();
        String startDate = LocalDateTime.now().plusDays(5).toString();
        MaintenanceType type = MaintenanceType.PREVENTIVE;
        
        mockMvc.perform(post("/api/maintenance/schedule")
                .param("vehicleId", vehicleId)
                .param("startDate", startDate)
                .param("type", type.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId", is(vehicleId)))
                .andExpect(jsonPath("$.type", is(type.toString())))
                .andExpect(jsonPath("$.completed", is(false)));
    }

    @Test
    public void testCompleteMaintenance() throws Exception {
        mockMvc.perform(put("/api/maintenance/{id}/complete", testMaintenance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testMaintenance.getId().intValue())))
                .andExpect(jsonPath("$.endDate", notNullValue()));
        
        // Test with non-existent ID
        mockMvc.perform(put("/api/maintenance/9999/complete"))
                .andExpect(status().isNotFound());
    }
}
