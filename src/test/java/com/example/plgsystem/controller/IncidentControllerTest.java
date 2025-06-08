package com.example.plgsystem.controller;

import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.IncidentType;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.model.VehicleType;
import com.example.plgsystem.repository.IncidentRepository;
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
public class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Vehicle testVehicle;
    private Incident testIncident;

    @BeforeEach
    public void setup() {
        // Create a test vehicle
        Position position = new Position(40, 50);
        testVehicle = new Vehicle("INC-TEST-001", VehicleType.TA, position);
        testVehicle = vehicleRepository.save(testVehicle);
        
        // Create a test incident
        LocalDateTime now = LocalDateTime.now();
        testIncident = new Incident();
        testIncident.setVehicleId(testVehicle.getId());
        testIncident.setPosition(position);
        testIncident.setTimestamp(now);
        testIncident.setType(IncidentType.TYPE_2);
        testIncident.setDescription("Test mechanical failure");
        testIncident.setResolved(false);
        
        testIncident = incidentRepository.save(testIncident);
    }

    @Test
    public void testGetAllIncidents() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].vehicleId", is(testVehicle.getId())))
                .andExpect(jsonPath("$[0].description", is("Test mechanical failure")));
    }

    @Test
    public void testGetIncidentById() throws Exception {
        mockMvc.perform(get("/api/incidents/{id}", testIncident.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testIncident.getId())))
                .andExpect(jsonPath("$.vehicleId", is(testVehicle.getId())))
                .andExpect(jsonPath("$.type", is(testIncident.getType().toString())));
        
        // Test with non-existent ID
        mockMvc.perform(get("/api/incidents/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetIncidentsByVehicle() throws Exception {
        // Create another vehicle and incident
        Position pos2 = new Position(60, 70);
        Vehicle vehicle2 = new Vehicle("INC-TEST-002", VehicleType.TA, pos2);
        vehicle2 = vehicleRepository.save(vehicle2);
        
        Incident incident2 = new Incident();
        incident2.setVehicleId(vehicle2.getId());
        incident2.setPosition(pos2);
        incident2.setTimestamp(LocalDateTime.now());
        incident2.setType(IncidentType.TYPE_3);
        incident2.setDescription("Another vehicle incident");
        incident2.setResolved(false);
        
        incidentRepository.save(incident2);
        
        // Test finding incidents for testVehicle
        mockMvc.perform(get("/api/incidents/vehicle/{vehicleId}", testVehicle.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].vehicleId", is(testVehicle.getId())));
        
        // Test with non-existent vehicle ID
        mockMvc.perform(get("/api/incidents/vehicle/NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testGetIncidentsByType() throws Exception {
        // Create another incident of different type
        Incident accident = new Incident();
        accident.setVehicleId(testVehicle.getId());
        accident.setPosition(testVehicle.getCurrentPosition());
        accident.setTimestamp(LocalDateTime.now());
        accident.setType(IncidentType.TYPE_3);
        accident.setDescription("Accident incident");
        accident.setResolved(false);
        
        incidentRepository.save(accident);
        
        // Test finding incidents by type
        mockMvc.perform(get("/api/incidents/type/{type}", IncidentType.TYPE_2))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].type", is(IncidentType.TYPE_2.toString())));
        
        // Test finding another type
        mockMvc.perform(get("/api/incidents/type/{type}", IncidentType.TYPE_3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].type", is(IncidentType.TYPE_3.toString())));
    }

    @Test
    public void testGetActiveIncidents() throws Exception {
        // Create a resolved incident
        Incident resolved = new Incident();
        resolved.setVehicleId(testVehicle.getId());
        resolved.setPosition(testVehicle.getCurrentPosition());
        resolved.setTimestamp(LocalDateTime.now().minusHours(2));
        resolved.setType(IncidentType.TYPE_2);
        resolved.setDescription("Resolved incident");
        resolved.setResolved(true);
        
        incidentRepository.save(resolved);
        
        // Test finding active incidents (unresolved)
        mockMvc.perform(get("/api/incidents/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].description", is("Test mechanical failure")))
                .andExpect(jsonPath("$[0].resolved", is(false)));
    }

    @Test
    public void testGetActiveIncidentsForVehicle() throws Exception {
        // Create another vehicle with active incident
        Position pos2 = new Position(80, 90);
        Vehicle vehicle2 = new Vehicle("INC-TEST-003", VehicleType.TA, pos2);
        vehicle2 = vehicleRepository.save(vehicle2);
        
        Incident active2 = new Incident();
        active2.setVehicleId(vehicle2.getId());
        active2.setPosition(pos2);
        active2.setTimestamp(LocalDateTime.now());
        active2.setType(IncidentType.TYPE_3);
        active2.setDescription("Vehicle 2 active incident");
        active2.setResolved(false);
        
        incidentRepository.save(active2);
        
        // Test finding active incidents for testVehicle
        mockMvc.perform(get("/api/incidents/active/vehicle/{vehicleId}", testVehicle.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].vehicleId", is(testVehicle.getId())));
        
        // Test with non-existent vehicle ID
        mockMvc.perform(get("/api/incidents/active/vehicle/NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testGetResolvedIncidents() throws Exception {
        // Create a resolved incident
        Incident resolved = new Incident();
        resolved.setVehicleId(testVehicle.getId());
        resolved.setPosition(testVehicle.getCurrentPosition());
        resolved.setTimestamp(LocalDateTime.now().minusHours(3));
        resolved.setType(IncidentType.TYPE_2);
        resolved.setDescription("Resolved incident");
        resolved.setResolved(true);
        
        incidentRepository.save(resolved);
        
        // Test finding resolved incidents
        mockMvc.perform(get("/api/incidents/resolved"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$[0].description", is("Resolved incident")))
                .andExpect(jsonPath("$[0].resolved", is(true)));
    }

    @Test
    public void testGetIncidentsByDateRange() throws Exception {
        // Create an incident from yesterday
        Incident yesterday = new Incident();
        yesterday.setVehicleId(testVehicle.getId());
        yesterday.setPosition(testVehicle.getCurrentPosition());
        yesterday.setTimestamp(LocalDateTime.now().minusDays(1));
        yesterday.setType(IncidentType.TYPE_3);
        yesterday.setDescription("Yesterday incident");
        yesterday.setResolved(true);
        
        incidentRepository.save(yesterday);
        
        // Test finding incidents from yesterday to tomorrow
        String startDate = LocalDateTime.now().minusDays(2).toString();
        String endDate = LocalDateTime.now().plusDays(1).toString();
        
        mockMvc.perform(get("/api/incidents/date-range")
                .param("startDate", startDate)
                .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(2))))
                .andExpect(jsonPath("$[*].description", hasItems("Test mechanical failure", "Yesterday incident")));
    }

    @Test
    public void testCreateIncident() throws Exception {
        // Create a new incident
        Position position = new Position(100, 110);
        Incident newIncident = new Incident();
        newIncident.setVehicleId(testVehicle.getId());
        newIncident.setPosition(position);
        newIncident.setTimestamp(LocalDateTime.now());
        newIncident.setType(IncidentType.TYPE_3);
        newIncident.setDescription("New test incident");
        newIncident.setResolved(false);
        
        mockMvc.perform(post("/api/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newIncident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.vehicleId", is(testVehicle.getId())))
                .andExpect(jsonPath("$.description", is("New test incident")));
    }

    @Test
    public void testDeleteIncident() throws Exception {
        mockMvc.perform(delete("/api/incidents/{id}", testIncident.getId()))
                .andExpect(status().isOk());
        
        // Verify it's deleted
        mockMvc.perform(get("/api/incidents/{id}", testIncident.getId()))
                .andExpect(status().isNotFound());
        
        // Try to delete non-existent incident
        mockMvc.perform(delete("/api/incidents/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }
}
