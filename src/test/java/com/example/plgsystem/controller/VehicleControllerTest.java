package com.example.plgsystem.controller;

import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.model.VehicleStatus;
import com.example.plgsystem.model.VehicleType;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Vehicle testVehicle;

    @BeforeEach
    public void setup() {
        // Create a test vehicle
        Position position = new Position(20, 30);
        testVehicle = new Vehicle("VEH-TEST-001", VehicleType.TA, position);
        testVehicle.setCurrentGLP(1500.0);
        testVehicle = vehicleRepository.save(testVehicle);
    }

    @Test
    public void testGetAllVehicles() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].id", is("VEH-TEST-001")))
                .andExpect(jsonPath("$[0].type", is("TA")));
    }

    @Test
    public void testGetVehicleById() throws Exception {
        mockMvc.perform(get("/api/vehicles/{id}", testVehicle.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testVehicle.getId())))
                .andExpect(jsonPath("$.type", is("TA")))
                .andExpect(jsonPath("$.currentGLP", is(1500.0)));
        
        // Test with non-existent ID
        mockMvc.perform(get("/api/vehicles/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetAvailableVehicles() throws Exception {
        // Create a vehicle in maintenance
        Position pos2 = new Position(40, 50);
        Vehicle maintenanceVehicle = new Vehicle("VEH-MAINT-001", VehicleType.TA, pos2);
        maintenanceVehicle.setStatus(VehicleStatus.MAINTENANCE);
        vehicleRepository.save(maintenanceVehicle);
        
        // Test finding available vehicles
        mockMvc.perform(get("/api/vehicles/available"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].id", is("VEH-TEST-001")))
                .andExpect(jsonPath("$[0].status", is("AVAILABLE")));
    }

    @Test
    public void testGetVehiclesByStatus() throws Exception {
        // Create a vehicle with IN_TRANSIT status
        Position pos2 = new Position(60, 70);
        Vehicle transitVehicle = new Vehicle("VEH-TRANSIT-001", VehicleType.TA, pos2);
        transitVehicle.setStatus(VehicleStatus.IN_TRANSIT);
        vehicleRepository.save(transitVehicle);
        
        // Test finding vehicles by status
        mockMvc.perform(get("/api/vehicles/status/{status}", VehicleStatus.AVAILABLE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].id", is("VEH-TEST-001")));
        
        mockMvc.perform(get("/api/vehicles/status/{status}", VehicleStatus.IN_TRANSIT))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].id", is("VEH-TRANSIT-001")));
    }

    @Test
    public void testGetVehiclesByType() throws Exception {
        // Create a vehicle of type TB
        Position pos2 = new Position(80, 90);
        Vehicle cmVehicle = new Vehicle("VEH-TB-001", VehicleType.TB, pos2);
        vehicleRepository.save(cmVehicle);
        
        // Test finding vehicles by type
        mockMvc.perform(get("/api/vehicles/type/{type}", VehicleType.TA))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].id", is("VEH-TEST-001")));
        
        mockMvc.perform(get("/api/vehicles/type/{type}", VehicleType.TB))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].id", is("VEH-TB-001")));
    }

    @Test
    public void testGetVehiclesByRadius() throws Exception {
        // Create vehicles at different locations
        
        // Vehicle near the center
        Position pos1 = new Position(55, 55);
        Vehicle nearVehicle = new Vehicle("VEH-NEAR-001", VehicleType.TA, pos1);
        vehicleRepository.save(nearVehicle);
        
        // Vehicle far from the center
        Position pos2 = new Position(90, 90);
        Vehicle farVehicle = new Vehicle("VEH-FAR-001", VehicleType.TA, pos2);
        vehicleRepository.save(farVehicle);
        
        // Test finding vehicles within 10 distance units from center
        mockMvc.perform(get("/api/vehicles/radius")
                .param("x", "50")
                .param("y", "50")
                .param("radius", "10.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].id", is("VEH-NEAR-001")));
        
        // Test finding vehicles within 60 distance units from center
        mockMvc.perform(get("/api/vehicles/radius")
                .param("x", "50")
                .param("y", "50")
                .param("radius", "60.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(3))));
    }

    @Test
    public void testCreateVehicle() throws Exception {
        // Create a new vehicle
        Position pos = new Position(100, 110);
        Vehicle newVehicle = new Vehicle("VEH-NEW-001", VehicleType.TB, pos);
        newVehicle.setCurrentGLP(2000.0);
        
        mockMvc.perform(post("/api/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newVehicle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("VEH-NEW-001")))
                .andExpect(jsonPath("$.type", is("TB")))
                .andExpect(jsonPath("$.currentGLP", is(2000.0)));
    }

    @Test
    public void testUpdateVehiclePosition() throws Exception {
        // Update vehicle position
        Position newPosition = new Position(120, 130);
        
        mockMvc.perform(put("/api/vehicles/{id}/position", testVehicle.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newPosition)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testVehicle.getId())))
                .andExpect(jsonPath("$.currentPosition.x", is(120)))
                .andExpect(jsonPath("$.currentPosition.y", is(130)));
    }

    @Test
    public void testUpdateVehicleStatus() throws Exception {
        // Update vehicle status to IN_TRANSIT
        mockMvc.perform(put("/api/vehicles/{id}/status", testVehicle.getId())
                .param("status", VehicleStatus.IN_TRANSIT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testVehicle.getId())))
                .andExpect(jsonPath("$.status", is("IN_TRANSIT")));
        
        // Update vehicle status to MAINTENANCE
        mockMvc.perform(put("/api/vehicles/{id}/status", testVehicle.getId())
                .param("status", VehicleStatus.MAINTENANCE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));
    }

    @Test
    public void testUpdateGLPLevel() throws Exception {
        // Add GLP to vehicle
        double addedAmount = 500.0;
        
        mockMvc.perform(put("/api/vehicles/{id}/glp", testVehicle.getId())
                .param("amount", String.valueOf(addedAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testVehicle.getId())))
                .andExpect(jsonPath("$.currentGLP", is(2000.0)));  // 1500 + 500
        
        // Consume GLP
        mockMvc.perform(put("/api/vehicles/{id}/glp", testVehicle.getId())
                .param("amount", "-300.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGLP", is(1700.0)));  // 2000 - 300
    }

    @Test
    public void testDeleteVehicle() throws Exception {
        mockMvc.perform(delete("/api/vehicles/{id}", testVehicle.getId()))
                .andExpect(status().isOk());
        
        // Verify it's deleted
        mockMvc.perform(get("/api/vehicles/{id}", testVehicle.getId()))
                .andExpect(status().isNotFound());
        
        // Try to delete non-existent vehicle
        mockMvc.perform(delete("/api/vehicles/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }
}
