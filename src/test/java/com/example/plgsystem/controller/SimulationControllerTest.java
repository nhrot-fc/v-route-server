package com.example.plgsystem.controller;

import com.example.plgsystem.dto.SimulationCreateDTO;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.SimulationStatus;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
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
    private UUID simulationId;

    @BeforeEach
    public void setUp() {
        // Set up a test simulation with a simple state
        simulationId = UUID.randomUUID();

        Depot mainDepot = new Depot("CENTRAL", new Position(12, 8), 1000, DepotType.MAIN);
        mainDepot.refill();

        // Empty vehicles list
        // Empty aux depots
        SimulationState testState = new SimulationState(
                Collections.emptyList(), // Empty vehicles list
                mainDepot,
                new ArrayList<>(), // Empty aux depots
                LocalDateTime.now());

        testSimulation = mock(Simulation.class);
        when(testSimulation.getId()).thenReturn(simulationId);
        when(testSimulation.getState()).thenReturn(testState);
        when(testSimulation.getRealStartTime()).thenReturn(LocalDateTime.now());
        when(testSimulation.getRealEndTime()).thenReturn(null);
        when(testSimulation.getStatus()).thenReturn(SimulationStatus.RUNNING);
        when(testSimulation.getType()).thenReturn(SimulationType.CUSTOM);
        when(testSimulation.getCreationTime()).thenReturn(LocalDateTime.now());

        // Mock simulation service methods
        when(simulationService.getSimulation(any(UUID.class))).thenReturn(testSimulation);
        when(simulationService.startSimulation(any(UUID.class))).thenReturn(testSimulation);
        when(simulationService.pauseSimulation(any(UUID.class))).thenReturn(testSimulation);
        when(simulationService.finishSimulation(any(UUID.class))).thenReturn(testSimulation);
        doNothing().when(simulationService).replanSimulation(any(Simulation.class));
        try {
            doNothing().when(simulationService).loadOrders(any(Simulation.class), anyInt(), anyInt(), any());
            doNothing().when(simulationService).loadBlockages(any(Simulation.class), anyInt(), anyInt(), any());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Mock createSimplifiedSimulation
        when(simulationService.createSimulation(
                any(SimulationType.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt())).thenReturn(testSimulation);

        // Setup MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
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
        // Create the request body with the new format
        SimulationCreateDTO createDTO = new SimulationCreateDTO();
        createDTO.setTaVehicles(1);
        createDTO.setTbVehicles(1);
        createDTO.setTcVehicles(1);
        createDTO.setTdVehicles(1);
        createDTO.setStartDateTime(LocalDateTime.now());
        createDTO.setEndDateTime(LocalDateTime.now().plusDays(1));
        createDTO.setType(SimulationType.CUSTOM);

        // Convert to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // To handle LocalDateTime
        String requestBody = objectMapper.writeValueAsString(createDTO);

        // Perform POST request to create simulation
        mockMvc.perform(post("/api/simulation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        // Verify the service was called correctly with new method
        verify(simulationService).createSimulation(
                eq(SimulationType.CUSTOM),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(1), // taVehicles
                eq(1), // tbVehicles
                eq(1), // tcVehicles
                eq(1) // tdVehicles
        );
    }

    @Test
    public void testSimulationLifecycle() throws Exception {
        // 1. Start simulation
        mockMvc.perform(post("/api/simulation/{id}/start", simulationId))
                .andExpect(status().isOk());
        verify(simulationService).startSimulation(simulationId);

        // 2. Pause simulation
        mockMvc.perform(post("/api/simulation/{id}/pause", simulationId))
                .andExpect(status().isOk());
        verify(simulationService).pauseSimulation(simulationId);

        // 3. Stop simulation
        mockMvc.perform(post("/api/simulation/{id}/stop", simulationId))
                .andExpect(status().isOk());
        verify(simulationService).finishSimulation(simulationId);
    }

    @Test
    public void testReplanSimulation() throws Exception {
        // Test replan simulation
        mockMvc.perform(post("/api/simulation/{id}/replan", simulationId))
                .andExpect(status().isOk());

        verify(simulationService).getSimulation(simulationId);
        verify(simulationService).replanSimulation(testSimulation);
    }

    @Test
    public void testReplanSimulation_NotFound() throws Exception {
        // Mock the simulation service to return null for a non-existent simulation
        when(simulationService.getSimulation(any(UUID.class))).thenReturn(null);

        // Test replan for non-existent simulation
        mockMvc.perform(post("/api/simulation/{id}/replan", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testLoadOrders() throws Exception {
        // Create mock file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "01d00h24m:16,13,c-198,3m3,4h\n01d00h48m:5,18,c-12,9m3,17h".getBytes());

        // Test load orders
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/simulation/{id}/load-orders", simulationId);

        mockMvc.perform(builder
                .file(file)
                .param("year", "2025")
                .param("month", "1"))
                .andExpect(status().isOk());

        verify(simulationService).getSimulation(simulationId);
        verify(simulationService).loadOrders(eq(testSimulation), eq(2025), eq(1), any());
    }

    @Test
    public void testLoadOrders_NotFound() throws Exception {
        // Mock the simulation service to return null for a non-existent simulation
        when(simulationService.getSimulation(any(UUID.class))).thenReturn(null);

        // Create mock file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "01d00h24m:16,13,c-198,3m3,4h".getBytes());

        // Test load orders for non-existent simulation
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/simulation/{id}/load-orders",
                UUID.randomUUID());

        mockMvc.perform(builder
                .file(file)
                .param("year", "2025")
                .param("month", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testLoadBlockages() throws Exception {
        // Create mock file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blockages.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "01d00h31m-01d21h35m:15,10,30,10,30,18\n01d01h13m-01d20h38m:08,03,08,23,20,23".getBytes());

        // Test load blockages
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/simulation/{id}/load-blockages", simulationId);

        mockMvc.perform(builder
                .file(file)
                .param("year", "2025")
                .param("month", "1"))
                .andExpect(status().isOk());

        verify(simulationService).getSimulation(simulationId);
        verify(simulationService).loadBlockages(eq(testSimulation), eq(2025), eq(1), any());
    }

    @Test
    public void testLoadBlockages_NotFound() throws Exception {
        // Mock the simulation service to return null for a non-existent simulation
        when(simulationService.getSimulation(any(UUID.class))).thenReturn(null);

        // Create mock file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blockages.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "01d00h31m-01d21h35m:15,10,30,10,30,18".getBytes());

        // Test load blockages for non-existent simulation
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/simulation/{id}/load-blockages",
                UUID.randomUUID());

        mockMvc.perform(builder
                .file(file)
                .param("year", "2025")
                .param("month", "1"))
                .andExpect(status().isNotFound());
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

    @Test
    public void testGetSimulation() throws Exception {
        // Setup a specific test UUID for clarity
        UUID specificTestId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        
        // Mock the simulation service to return our test simulation for this specific ID
        when(simulationService.getSimulation(eq(specificTestId))).thenReturn(testSimulation);
        
        // Perform GET request and expect 200 OK with simulation data
        mockMvc.perform(get("/api/simulation/{id}", specificTestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(simulationId.toString()))
                .andExpect(jsonPath("$.type").value(SimulationType.CUSTOM.toString()))
                .andExpect(jsonPath("$.status").value(SimulationStatus.RUNNING.toString()));
        
        // Verify the service method was called with the correct ID
        verify(simulationService, times(1)).getSimulation(specificTestId);
    }
    
    @Test
    public void testGetSimulation_WithStringId() throws Exception {
        // This test specifically checks the conversion of string IDs to UUID
        // which might be the source of the failure mentioned by the user
        String stringId = "11111111-2222-3333-4444-555555555555";
        UUID uuidFromString = UUID.fromString(stringId);
        
        // Mock the simulation service to return our test simulation for the parsed UUID
        when(simulationService.getSimulation(eq(uuidFromString))).thenReturn(testSimulation);
        
        // Perform GET request using the string ID and expect 200 OK
        mockMvc.perform(get("/api/simulation/{id}", stringId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(simulationId.toString()));
        
        // Verify the service method was called with the UUID parsed from the string
        verify(simulationService, times(1)).getSimulation(uuidFromString);
    }
}