package com.example.plgsystem.controller;

import com.example.plgsystem.dto.SimulationCreateDTO;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.SimulationStatus;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.service.DepotService;
import com.example.plgsystem.service.SimulationService;
import com.example.plgsystem.service.VehicleService;
import com.example.plgsystem.simulation.Simulation;
import com.example.plgsystem.simulation.SimulationState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SimulationController.class)
public class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private SimulationService simulationService;

    @MockitoBean
    private VehicleService vehicleService;

    @MockitoBean
    private DepotService depotService;

    private Simulation testSimulation;
    private SimulationState testState;
    private UUID simulationId;

    @BeforeEach
    public void setUp() {
        // Set up a test simulation with a simple state
        simulationId = UUID.randomUUID();

        Depot mainDepot = new Depot("CENTRAL", new Position(12, 8), 1000, DepotType.MAIN);
        mainDepot.refill();

        testState = new SimulationState(
                Collections.emptyList(), // Empty vehicles list
                mainDepot,
                new ArrayList<>(), // Empty aux depots
                LocalDateTime.now());

        testSimulation = mock(Simulation.class);
        when(testSimulation.getId()).thenReturn(simulationId);
        when(testSimulation.getState()).thenReturn(testState);
        when(testSimulation.getStartTime()).thenReturn(LocalDateTime.now());
        when(testSimulation.getStatus()).thenReturn(SimulationStatus.RUNNING);
        when(testSimulation.getType()).thenReturn(SimulationType.CUSTOM);

        // Mock simulation service methods
        when(simulationService.getSimulation(any(UUID.class))).thenReturn(testSimulation);
        when(simulationService.getDailyOperations()).thenReturn(testSimulation);
        when(simulationService.startSimulation(any(UUID.class))).thenReturn(testSimulation);
        when(simulationService.pauseSimulation(any(UUID.class))).thenReturn(testSimulation);
        when(simulationService.finishSimulation(any(UUID.class))).thenReturn(testSimulation);

        // Create simulation mock
        when(simulationService.createTimeBasedSimulation(
                any(SimulationType.class),
                anyList(),
                any(Depot.class),
                anyList(),
                any(LocalDateTime.class))).thenReturn(testSimulation);

        // Setup MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testGetSimulationById() throws Exception {
        // Perform GET request to retrieve simulation by ID
        mockMvc.perform(get("/api/simulation/{id}", simulationId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Verify the service was called correctly
        verify(simulationService).getSimulation(simulationId);
    }

    @Test
    public void testGetSimulationById_NotFound() throws Exception {
        // Mock the simulation service to return null for a non-existent simulation
        when(simulationService.getSimulation(any(UUID.class))).thenReturn(null);

        // Perform GET request and expect 404
        mockMvc.perform(get("/api/simulation/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateSimulation() throws Exception {
        // Mock vehicle and depot service methods
        Vehicle vehicle1 = mock(Vehicle.class);
        Depot mainDepot = mock(Depot.class);
        Depot auxDepot = mock(Depot.class);

        when(vehicleService.findById("v1")).thenReturn(Optional.of(vehicle1));
        when(depotService.findById("d1")).thenReturn(Optional.of(mainDepot));
        when(depotService.findById("a1")).thenReturn(Optional.of(auxDepot));

        // Create the request body
        SimulationCreateDTO createDTO = new SimulationCreateDTO();
        createDTO.setVehicleIds(List.of("v1"));
        createDTO.setMainDepotId("d1");
        createDTO.setAuxDepotIds(List.of("a1"));
        createDTO.setStartDateTime(LocalDateTime.now());

        // Convert to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // To handle LocalDateTime
        String requestBody = objectMapper.writeValueAsString(createDTO);

        // Perform POST request to create simulation
        mockMvc.perform(post("/api/simulation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .param("type", "CUSTOM"))
                .andExpect(status().isCreated())
                .andReturn();

        // Verify the service was called correctly
        verify(simulationService).createTimeBasedSimulation(
                eq(SimulationType.CUSTOM),
                anyList(),
                eq(mainDepot),
                anyList(),
                any(LocalDateTime.class));
    }

    @Test
    public void testGetSimulationState() throws Exception {
        // Perform GET request to retrieve simulation state
        mockMvc.perform(get("/api/simulation/{id}/state", simulationId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Verify the service was called correctly
        verify(simulationService).getSimulation(simulationId);
    }

    @Test
    public void testSimulationLifecycle() throws Exception {
        // 1. Get the simulation
        mockMvc.perform(get("/api/simulation/{id}", simulationId))
                .andExpect(status().isOk());

        // 2. Start simulation
        mockMvc.perform(post("/api/simulation/{id}/start", simulationId))
                .andExpect(status().isOk());

        verify(simulationService).startSimulation(simulationId);

        // 3. Pause simulation
        mockMvc.perform(post("/api/simulation/{id}/pause", simulationId))
                .andExpect(status().isOk());

        verify(simulationService).pauseSimulation(simulationId);

        // 4. Stop simulation
        mockMvc.perform(post("/api/simulation/{id}/stop", simulationId))
                .andExpect(status().isOk());

        verify(simulationService).finishSimulation(simulationId);
    }

    @Test
    public void testGetDailyOperations() throws Exception {
        // Perform GET request for daily operations
        mockMvc.perform(get("/api/simulation/daily"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(simulationService).getDailyOperations();
    }

    @Test
    public void testGetDailyOperationsState() throws Exception {
        // Perform GET request for daily operations state
        mockMvc.perform(get("/api/simulation/daily/state"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(simulationService).getDailyOperations();
    }

    @Test
    public void testListSimulations() throws Exception {
        // Mock getAllSimulations to return a map with our test simulation
        Map<UUID, Simulation> simulationsMap = new HashMap<>();
        simulationsMap.put(simulationId, testSimulation);
        when(simulationService.getAllSimulations()).thenReturn(simulationsMap);

        // Perform GET request to list all simulations
        mockMvc.perform(get("/api/simulation"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(simulationService).getAllSimulations();
    }
}