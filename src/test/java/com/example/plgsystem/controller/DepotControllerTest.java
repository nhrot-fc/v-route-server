package com.example.plgsystem.controller;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.repository.DepotRepository;
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
public class DepotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepotRepository depotRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Depot testDepot;

    @BeforeEach
    public void setup() {
        // Create a test depot
        Position position = new Position(20, 30);
        testDepot = new Depot("DEPOT-TEST-001", position, 30000.0, 5000.0);
        testDepot.setCurrentGLP(20000.0);
        testDepot = depotRepository.save(testDepot);
    }

    @Test
    public void testGetAllDepots() throws Exception {
        mockMvc.perform(get("/api/depots"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].id", is("DEPOT-TEST-001")))
                .andExpect(jsonPath("$[0].glpCapacity", is(30000.0)))
                .andExpect(jsonPath("$[0].currentGLP", is(20000.0)));
    }

    @Test
    public void testGetDepotById() throws Exception {
        mockMvc.perform(get("/api/depots/{id}", testDepot.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testDepot.getId())))
                .andExpect(jsonPath("$.glpCapacity", is(30000.0)))
                .andExpect(jsonPath("$.currentGLP", is(20000.0)));
        
        // Test with non-existent ID
        mockMvc.perform(get("/api/depots/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetDepotsByAvailableCapacity() throws Exception {
        // Create additional depots with different capacity levels
        Position pos1 = new Position(40, 50);
        Depot lowCapacityDepot = new Depot("DEPOT-LOW-001", pos1, 10000.0, 1000.0);
        lowCapacityDepot.setCurrentGLP(9000.0); // Only 1000 available
        depotRepository.save(lowCapacityDepot);
        
        Position pos2 = new Position(60, 70);
        Depot highCapacityDepot = new Depot("DEPOT-HIGH-001", pos2, 50000.0, 10000.0);
        highCapacityDepot.setCurrentGLP(20000.0); // 30000 available
        depotRepository.save(highCapacityDepot);
        
        // Test finding depots with at least 5000 capacity available
        mockMvc.perform(get("/api/depots/available")
                .param("minCapacity", "5000.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(2))))
                .andExpect(jsonPath("$[*].id", hasItems("DEPOT-TEST-001", "DEPOT-HIGH-001")));
        
        // Test finding depots with at least 15000 capacity available
        mockMvc.perform(get("/api/depots/available")
                .param("minCapacity", "15000.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].id", is("DEPOT-HIGH-001")));
    }

    @Test
    public void testGetDepotsByRadius() throws Exception {
        // Depot near the center
        Position pos1 = new Position(55, 55);
        Depot nearDepot = new Depot("DEPOT-NEAR-001", pos1, 20000.0, 2000.0);
        nearDepot.setCurrentGLP(15000.0);
        depotRepository.save(nearDepot);
        
        // Depot far from the center
        Position pos2 = new Position(90, 90);
        Depot farDepot = new Depot("DEPOT-FAR-001", pos2, 20000.0, 2000.0);
        farDepot.setCurrentGLP(15000.0);
        depotRepository.save(farDepot);
        
        // Test finding depots within 10 distance units from center
        mockMvc.perform(get("/api/depots/radius")
                .param("x", "50")
                .param("y", "50")
                .param("radius", "10.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].id", is("DEPOT-NEAR-001")));
        
        // Test finding depots within 60 distance units from center
        mockMvc.perform(get("/api/depots/radius")
                .param("x", "50")
                .param("y", "50")
                .param("radius", "60.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(3))));
    }

    @Test
    public void testCreateDepot() throws Exception {
        // Create a new depot
        Position pos = new Position(100, 110);
        Depot newDepot = new Depot("DEPOT-NEW-001", pos, 40000.0, 8000.0);
        newDepot.setCurrentGLP(30000.0);
        
        mockMvc.perform(post("/api/depots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newDepot)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("DEPOT-NEW-001")))
                .andExpect(jsonPath("$.glpCapacity", is(40000.0)))
                .andExpect(jsonPath("$.currentGLP", is(30000.0)));
    }

    @Test
    public void testUpdateGLPLevel() throws Exception {
        // Add GLP to depot
        double addedAmount = 5000.0;
        
        mockMvc.perform(put("/api/depots/{id}/glp", testDepot.getId())
                .param("amount", String.valueOf(addedAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testDepot.getId())))
                .andExpect(jsonPath("$.currentGLP", is(25000.0)));  // 20000 + 5000
        
        // Extract GLP from depot
        mockMvc.perform(put("/api/depots/{id}/glp", testDepot.getId())
                .param("amount", "-10000.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGLP", is(15000.0)));  // 25000 - 10000
        
        // Try to add more than capacity
        mockMvc.perform(put("/api/depots/{id}/glp", testDepot.getId())
                .param("amount", "20000.0"))
                .andExpect(status().isBadRequest());
        
        // Try to extract more than available
        mockMvc.perform(put("/api/depots/{id}/glp", testDepot.getId())
                .param("amount", "-20000.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateMinimumThreshold() throws Exception {
        // Update minimum threshold
        double newThreshold = 8000.0;
        
        mockMvc.perform(put("/api/depots/{id}/threshold", testDepot.getId())
                .param("threshold", String.valueOf(newThreshold)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testDepot.getId())))
                .andExpect(jsonPath("$.glpMinThreshold", is(8000.0)));
    }

    @Test
    public void testDeleteDepot() throws Exception {
        mockMvc.perform(delete("/api/depots/{id}", testDepot.getId()))
                .andExpect(status().isOk());
        
        // Verify it's deleted
        mockMvc.perform(get("/api/depots/{id}", testDepot.getId()))
                .andExpect(status().isNotFound());
        
        // Try to delete non-existent depot
        mockMvc.perform(delete("/api/depots/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }
}
