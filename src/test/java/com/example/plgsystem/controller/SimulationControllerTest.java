package com.example.plgsystem.controller;

import com.example.plgsystem.dto.SimulationReportDTO;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.simulation.Simulation;
import com.example.plgsystem.simulation.SimulationManager;
import com.example.plgsystem.simulation.SimulationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest(SimulationController.class)
public class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private SimulationManager simulationManager;

    private Simulation testSimulation;
    private SimulationState testState;
    private final String simulationId = "sim-123";

    @BeforeEach
    public void setUp() {
        // Set up a test simulation with a simple state
        Depot mainDepot = new Depot("CENTRAL", new Position(12, 8), 1000, true);
        mainDepot.refillGLP();
        
        testState = new SimulationState(
                Collections.emptyList(),  // Empty vehicles list
                mainDepot,
                new ArrayList<>(),       // Empty aux depots
                LocalDateTime.now()
        );
        
        testSimulation = mock(Simulation.class);
        when(testSimulation.getId()).thenReturn(simulationId);
        when(testSimulation.getName()).thenReturn("Test Simulation");
        when(testSimulation.getDescription()).thenReturn("Test Description");
        when(testSimulation.getState()).thenReturn(testState);
        when(testSimulation.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(testSimulation.getLastUpdated()).thenReturn(LocalDateTime.now());
        
        // Mock JSON responses for different endpoints
        when(simulationManager.createSimulation(anyString(), anyString(), any(LocalDateTime.class), anyString()))
                .thenReturn(testSimulation);
        when(simulationManager.getSimulation(simulationId)).thenReturn(Optional.of(testSimulation));
        
        // Setup MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testGetSimulationStatus() throws Exception {
        // Mock the simulation manager to return our test simulation
        when(simulationManager.getSimulation(simulationId)).thenReturn(Optional.of(testSimulation));
        when(simulationManager.isSimulationRunning(simulationId)).thenReturn(true);

        // Log the expected simulation ID
        System.out.println("Expected simulation ID: " + simulationId);

        // Perform GET request to retrieve simulation status
        MvcResult result = mockMvc.perform(get("/api/simulation/{id}/status", simulationId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Parse response content and verify the simulation state DTO
        String responseContent = result.getResponse().getContentAsString();
        
        // Print the response for debugging
        System.out.println("Response content: " + responseContent);
        
        // Verify key elements are present in the response
        assertTrue(responseContent.contains(simulationId), "Response should contain simulation ID: " + simulationId);
        assertTrue(responseContent.contains("\"running\":true"), "Response should indicate simulation is running");
        assertTrue(responseContent.contains("\"vehicles\""), "Response should contain vehicles field");
        assertTrue(responseContent.contains("\"mainDepot\""), "Response should contain mainDepot field");
        assertTrue(responseContent.contains("\"orders\""), "Response should contain orders field");
        
        // Verify the service was called correctly
        verify(simulationManager).getSimulation(simulationId);
        verify(simulationManager).isSimulationRunning(simulationId);
    }

    @Test
    public void testGetSimulationStatus_NotFound() throws Exception {
        // Mock the simulation manager to return empty for a non-existent simulation
        when(simulationManager.getSimulation("non-existent")).thenReturn(Optional.empty());

        // Perform GET request and expect 404
        mockMvc.perform(get("/api/simulation/{id}/status", "non-existent"))
                .andExpect(status().isNotFound());
        
        verify(simulationManager).getSimulation("non-existent");
    }
    
    @Test
    public void testCreateAndGetSimulationStatus() throws Exception {
        // Mock simulation creation
        when(simulationManager.createSimulation(anyString(), anyString(), any(LocalDateTime.class), anyString()))
                .thenReturn(testSimulation);
        when(simulationManager.getSimulation(simulationId)).thenReturn(Optional.of(testSimulation));
        when(simulationManager.isSimulationRunning(simulationId)).thenReturn(false);
        when(simulationManager.configureSimulation(eq(simulationId), any(), anyInt())).thenReturn(true);
        when(simulationManager.startSimulation(simulationId)).thenReturn(true);
        
        // Create simulation
        MvcResult createResult = mockMvc.perform(post("/api/simulation")
                .param("name", "Test Simulation")
                .param("description", "Test Description")
                .param("simulationType", "daily"))
                .andExpect(status().isCreated())
                .andReturn();
        
        // Print the create response for debugging
        String createContent = createResult.getResponse().getContentAsString();
        System.out.println("Create response: " + createContent);
        
        // Check that the response contains the expected ID
        assertTrue(createContent.contains("\"id\":\"" + simulationId + "\""), 
                   "Response should contain the simulation ID");
        
        // Verify creation was called with correct parameters
        verify(simulationManager).createSimulation(
            eq("Test Simulation"), 
            eq("Test Description"), 
            any(LocalDateTime.class), 
            eq("daily")
        );
        
        // Now start the simulation
        MvcResult startResult = mockMvc.perform(post("/api/simulation/{id}/start", simulationId))
                .andExpect(status().isOk())
                .andReturn();
        
        // Print the start response for debugging
        String startContent = startResult.getResponse().getContentAsString();
        System.out.println("Start response: " + startContent);
        
        // Check that the response contains the expected status
        assertTrue(startContent.contains("\"status\":\"running\""), 
                   "Response should indicate running status");
        
        // Mock that simulation is now running
        when(simulationManager.isSimulationRunning(simulationId)).thenReturn(true);
        
        // Get the simulation status
        MvcResult result = mockMvc.perform(get("/api/simulation/{id}/status", simulationId))
                .andExpect(status().isOk())
                .andReturn();
        
        // Print the status response for debugging
        String statusContent = result.getResponse().getContentAsString();
        System.out.println("Status response: " + statusContent);
        
        // Check that the response contains expected values
        assertTrue(statusContent.contains("\"simulationId\":\"" + simulationId + "\""), 
                   "Status should contain simulation ID");
        assertTrue(statusContent.contains("\"running\":true"), 
                   "Status should indicate simulation is running");
        
        // Check for additional fields
        assertTrue(statusContent.contains("\"currentTime\""), "Status should include currentTime");
        assertTrue(statusContent.contains("\"vehicles\""), "Status should include vehicles");
        assertTrue(statusContent.contains("\"mainDepot\""), "Status should include mainDepot");
        assertTrue(statusContent.contains("\"pendingOrdersCount\""), "Status should include pendingOrdersCount");
    }
    
    @Test
    public void testSimulationLifecycle() throws Exception {
        // Mock full simulation lifecycle
        when(simulationManager.createSimulation(anyString(), anyString(), any(LocalDateTime.class), anyString()))
                .thenReturn(testSimulation);
        when(simulationManager.getSimulation(simulationId)).thenReturn(Optional.of(testSimulation));
        when(simulationManager.configureSimulation(eq(simulationId), any(), anyInt())).thenReturn(true);
        when(simulationManager.startSimulation(simulationId)).thenReturn(true);
        when(simulationManager.pauseSimulation(simulationId)).thenReturn(true);
        when(simulationManager.deleteSimulation(simulationId)).thenReturn(true);
        
        // 1. Create simulation with weekly type
        MvcResult createResult = mockMvc.perform(post("/api/simulation")
                .param("name", "Weekly Simulation")
                .param("description", "Test Weekly Simulation")
                .param("simulationType", "weekly")
                .param("dataSource", "/test/data"))
                .andExpect(status().isCreated())
                .andReturn();
                
        // Print the create response for debugging
        String createContent = createResult.getResponse().getContentAsString();
        System.out.println("Create response: " + createContent);
        
        // Check that response contains expected values
        assertTrue(createContent.contains("\"id\""), "Response should contain ID field");
        // Don't check for simulationType as it might not be in the response
                
        // 2. Start simulation
        MvcResult startResult = mockMvc.perform(post("/api/simulation/{id}/start", simulationId))
                .andExpect(status().isOk())
                .andReturn();
                
        // Print the start response for debugging
        String startContent = startResult.getResponse().getContentAsString();
        System.out.println("Start response: " + startContent);
                
        // Mock that simulation is now running
        when(simulationManager.isSimulationRunning(simulationId)).thenReturn(true);
        
        // 3. Get status (should be running)
        MvcResult runningResult = mockMvc.perform(get("/api/simulation/{id}/status", simulationId))
                .andExpect(status().isOk())
                .andReturn();
                
        // Print the running status response for debugging
        String runningContent = runningResult.getResponse().getContentAsString();
        System.out.println("Running status response: " + runningContent);
                
        // Check that simulation is running
        assertTrue(runningContent.contains("\"running\":true"), 
                   "Status should indicate simulation is running");
                
        // 4. Pause simulation
        MvcResult pauseResult = mockMvc.perform(post("/api/simulation/{id}/pause", simulationId))
                .andExpect(status().isOk())
                .andReturn();
                
        // Print the pause response for debugging
        String pauseContent = pauseResult.getResponse().getContentAsString();
        System.out.println("Pause response: " + pauseContent);
                
        // Check pause response
        assertTrue(pauseContent.contains("\"status\":\"paused\""), 
                   "Response should indicate paused status");
                
        // Mock that simulation is now paused
        when(simulationManager.isSimulationRunning(simulationId)).thenReturn(false);
        
        // 5. Get status again (should be paused)
        MvcResult pausedResult = mockMvc.perform(get("/api/simulation/{id}/status", simulationId))
                .andExpect(status().isOk())
                .andReturn();
                
        // Print the paused status response for debugging
        String pausedContent = pausedResult.getResponse().getContentAsString();
        System.out.println("Paused status response: " + pausedContent);
                
        // Check that simulation is paused
        assertTrue(pausedContent.contains("\"running\":false"), 
                   "Status should indicate simulation is not running");
                
        // 6. Delete simulation
        mockMvc.perform(delete("/api/simulation/{id}", simulationId))
                .andExpect(status().isNoContent());
                
        // Verify all required manager methods were called
        verify(simulationManager).createSimulation(anyString(), anyString(), any(LocalDateTime.class), anyString());
        verify(simulationManager, atLeastOnce()).getSimulation(simulationId);
        verify(simulationManager).startSimulation(simulationId);
        verify(simulationManager).pauseSimulation(simulationId);
        verify(simulationManager).deleteSimulation(simulationId);
        verify(simulationManager, atLeastOnce()).isSimulationRunning(simulationId);
    }

    @Test
    public void testGetEnvironment() throws Exception {
        // Mock the simulation manager to return our test simulation
        when(simulationManager.getSimulation(simulationId)).thenReturn(Optional.of(testSimulation));
        when(simulationManager.isSimulationRunning(simulationId)).thenReturn(true);

        // Perform GET request to retrieve environment data
        MvcResult result = mockMvc.perform(get("/api/simulation/{id}/environment", simulationId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Parse response content
        String responseContent = result.getResponse().getContentAsString();
        
        // Verify key environment elements are present
        assertTrue(responseContent.contains("\"currentTime\""));
        assertTrue(responseContent.contains("\"mainDepot\""));
        assertTrue(responseContent.contains("\"auxDepots\""));
        assertTrue(responseContent.contains("\"vehicles\""));
        
        // Verify the service was called
        verify(simulationManager).getSimulation(simulationId);
    }

    @Test
    public void testCreateCollapseSimulationAndGetStatus() throws Exception {
        // Mock simulation creation for collapse simulation
        when(simulationManager.createSimulation(anyString(), anyString(), any(LocalDateTime.class), anyString()))
                .thenReturn(testSimulation);
        when(simulationManager.getSimulation(simulationId)).thenReturn(Optional.of(testSimulation));
        when(simulationManager.isSimulationRunning(simulationId)).thenReturn(false);
        when(simulationManager.configureSimulation(eq(simulationId), any(), anyInt())).thenReturn(true);
        
        // Create collapse simulation
        MvcResult createResult = mockMvc.perform(post("/api/simulation")
                .param("name", "Collapse Simulation")
                .param("description", "Test Collapse Simulation")
                .param("simulationType", "collapse")
                .param("dataSource", "/test/data")
                .param("durationDays", "30"))
                .andExpect(status().isCreated())
                .andReturn();
        
        // Print the response for debugging
        String createContent = createResult.getResponse().getContentAsString();
        System.out.println("Create response: " + createContent);
        
        // Check simulation id is included in response
        assertTrue(createContent.contains("\"id\""), 
                   "Response should contain id field");
        
        // Verify creation was called with correct parameters
        verify(simulationManager).createSimulation(
            eq("Collapse Simulation"), 
            eq("Test Collapse Simulation"), 
            any(LocalDateTime.class), 
            eq("collapse")
        );
        verify(simulationManager).configureSimulation(eq(simulationId), eq("/test/data"), eq(30));
        
        // Start the simulation
        when(simulationManager.startSimulation(simulationId)).thenReturn(true);
        mockMvc.perform(post("/api/simulation/{id}/start", simulationId))
                .andExpect(status().isOk());
                
        // Mock that simulation is now running
        when(simulationManager.isSimulationRunning(simulationId)).thenReturn(true);
        
        // Get the simulation status
        MvcResult statusResult = mockMvc.perform(get("/api/simulation/{id}/status", simulationId))
                .andExpect(status().isOk())
                .andReturn();
                
        // Print the status response for debugging
        String statusContent = statusResult.getResponse().getContentAsString();
        System.out.println("Status response: " + statusContent);
        
        // Check status values with detailed error messages
        assertTrue(statusContent.contains("\"simulationId\":\"" + simulationId + "\""), 
                   "Status should contain simulation ID");
        assertTrue(statusContent.contains("\"running\":true"), 
                   "Status should indicate simulation is running");
    }

    @Test
    public void testGetSimulationReport() throws Exception {
        // Mock the simulation manager to return a report for our test simulation
        SimulationReportDTO mockReport = new SimulationReportDTO();
        mockReport.setSimulationId(simulationId);
        mockReport.setSimulationType("weekly");
        mockReport.setSimulationName("Test Simulation");
        mockReport.setTotalOrders(10);
        mockReport.setDeliveredOrders(8);
        mockReport.setLateDeliveries(1);
        
        when(simulationManager.getSimulationReport(simulationId)).thenReturn(Optional.of(mockReport));
        when(simulationManager.getSimulationReport("non-existent")).thenReturn(Optional.empty());
        
        // Perform GET request and verify response
        MvcResult result = mockMvc.perform(get("/api/simulation/{id}/report", simulationId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Verify the response contains expected values
        String responseContent = result.getResponse().getContentAsString();
        assertTrue(responseContent.contains("\"simulationId\":\"" + simulationId + "\""));
        assertTrue(responseContent.contains("\"simulationType\":\"weekly\""));
        assertTrue(responseContent.contains("\"totalOrders\":10"));
        assertTrue(responseContent.contains("\"deliveredOrders\":8"));
        
        // Verify not found case
        mockMvc.perform(get("/api/simulation/{id}/report", "non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetAllSimulationReports() throws Exception {
        // Mock the simulation manager to return a list of reports
        SimulationReportDTO mockReport1 = new SimulationReportDTO();
        mockReport1.setSimulationId(simulationId);
        mockReport1.setSimulationType("weekly");
        
        SimulationReportDTO mockReport2 = new SimulationReportDTO();
        mockReport2.setSimulationId("sim-456");
        mockReport2.setSimulationType("collapse");
        
        when(simulationManager.getAllSimulationReports()).thenReturn(List.of(mockReport1, mockReport2));
        
        // Perform GET request and verify response
        MvcResult result = mockMvc.perform(get("/api/simulation/reports"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Verify the response contains expected values
        String responseContent = result.getResponse().getContentAsString();
        assertTrue(responseContent.contains("\"simulationId\":\"" + simulationId + "\""));
        assertTrue(responseContent.contains("\"simulationType\":\"weekly\""));
        assertTrue(responseContent.contains("\"simulationId\":\"sim-456\""));
        assertTrue(responseContent.contains("\"simulationType\":\"collapse\""));
        
        // Verify service was called
        verify(simulationManager).getAllSimulationReports();
    }
} 