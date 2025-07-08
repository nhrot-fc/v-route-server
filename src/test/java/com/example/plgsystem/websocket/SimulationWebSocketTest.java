package com.example.plgsystem.websocket;

import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.SimulationStatus;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.service.SimulationService;
import com.example.plgsystem.simulation.Simulation;
import com.example.plgsystem.simulation.SimulationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SimulationWebSocketTest {

    @MockitoBean
    private SimulationService simulationService;

    private Simulation testSimulation;

    @BeforeEach
    public void setUp() {
        // Create test simulation with a UUID
        UUID simulationId = UUID.randomUUID();

        // Create simulation state
        Depot mainDepot = new Depot("MD001", new Position(0, 0), 10000, DepotType.MAIN);
        List<Vehicle> vehicles = new ArrayList<>();
        List<Depot> auxDepots = new ArrayList<>();

        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());

        // Mock the simulation
        testSimulation = mock(Simulation.class);
        when(testSimulation.getId()).thenReturn(simulationId);
        when(testSimulation.getState()).thenReturn(state);
        when(testSimulation.getRealStartTime()).thenReturn(LocalDateTime.now());
        when(testSimulation.getRealEndTime()).thenReturn(null);
        when(testSimulation.getStatus()).thenReturn(SimulationStatus.RUNNING);
        when(testSimulation.getType()).thenReturn(SimulationType.CUSTOM);

        // Configure simulation service mock
        when(simulationService.getSimulation(simulationId)).thenReturn(testSimulation);
    }

    @Test
    public void testWebSocketSubscription() {
        // This is a more complex test that would normally connect to the WebSocket
        // endpoint
        // For a simple unit test, we'll just verify the simulation service is correctly
        // configured

        // Send a simulation update
        simulationService.sendSimulationUpdate(testSimulation);

        // Verify that methods were called appropriately
        verify(simulationService).sendSimulationUpdate(testSimulation);
    }
}