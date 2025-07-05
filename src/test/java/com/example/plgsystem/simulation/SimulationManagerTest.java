package com.example.plgsystem.simulation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationManagerTest {

    private SimulationManager simulationManager;

    @BeforeEach
    public void setup() {
        simulationManager = new SimulationManager();
    }

    @Test
    public void testCreateAndGetSimulation() {
        // Given
        String name = "Test Simulation";
        String description = "A test simulation for unit testing";
        LocalDateTime startDateTime = LocalDateTime.now();
        
        // When
        Simulation simulation = simulationManager.createSimulation(name, description, startDateTime);
        Optional<Simulation> retrievedSimulation = simulationManager.getSimulation(simulation.getId());
        
        // Then
        assertTrue(retrievedSimulation.isPresent());
        assertEquals(simulation.getId(), retrievedSimulation.get().getId());
        assertEquals(name, retrievedSimulation.get().getName());
        assertEquals(description, retrievedSimulation.get().getDescription());
    }

    @Test
    public void testGetAllSimulations() {
        // When
        Simulation sim1 = simulationManager.createSimulation("Sim 1", "First simulation", LocalDateTime.now());
        Simulation sim2 = simulationManager.createSimulation("Sim 2", "Second simulation", LocalDateTime.now());
        
        List<Simulation> allSimulations = simulationManager.getAllSimulations();
        
        // Then
        assertEquals(2, allSimulations.size());
        assertTrue(allSimulations.stream().anyMatch(s -> s.getId().equals(sim1.getId())));
        assertTrue(allSimulations.stream().anyMatch(s -> s.getId().equals(sim2.getId())));
    }

    @Test
    public void testDeleteSimulation() {
        // Given
        Simulation simulation = simulationManager.createSimulation("Test", "Test Description", LocalDateTime.now());
        
        // When
        boolean deleteResult = simulationManager.deleteSimulation(simulation.getId());
        Optional<Simulation> retrievedSimulation = simulationManager.getSimulation(simulation.getId());
        
        // Then
        assertTrue(deleteResult);
        assertFalse(retrievedSimulation.isPresent());
    }

    @Test
    public void testSimulationRunningState() {
        // Given
        Simulation simulation = simulationManager.createSimulation("Test", "Test Description", LocalDateTime.now());
        
        // Initially not running
        assertFalse(simulationManager.isSimulationRunning(simulation.getId()));
        
        // When started
        boolean startResult = simulationManager.startSimulation(simulation.getId());
        
        // Then
        assertTrue(startResult);
        assertTrue(simulationManager.isSimulationRunning(simulation.getId()));
        
        // When paused
        boolean pauseResult = simulationManager.pauseSimulation(simulation.getId());
        
        // Then
        assertTrue(pauseResult);
        assertFalse(simulationManager.isSimulationRunning(simulation.getId()));
    }

    @Test
    public void testSetSimulationSpeed() {
        // Given
        Simulation simulation = simulationManager.createSimulation("Test", "Test Description", LocalDateTime.now());
        
        // When
        boolean result = simulationManager.setSimulationSpeed(simulation.getId(), 2);
        
        // Then
        assertTrue(result);
        
        // Invalid speed factor
        assertFalse(simulationManager.setSimulationSpeed(simulation.getId(), 0));
        assertFalse(simulationManager.setSimulationSpeed(simulation.getId(), -1));
    }

    @Test
    public void testVehicleBreakdownAndRepair() {
        // Given
        Simulation simulation = simulationManager.createSimulation("Test", "Test Description", LocalDateTime.now());
        
        // Verificar que hay vehículos disponibles
        assertFalse(simulation.getState().getVehicles().isEmpty(), "No hay vehículos disponibles para la prueba");
        
        String vehicleId = simulation.getState().getVehicles().get(0).getId();
        
        // When - simulate breakdown
        boolean breakdownResult = simulationManager.simulateVehicleBreakdown(simulation.getId(), vehicleId);
        
        // Then
        assertTrue(breakdownResult, "La simulación de avería falló");
        
        // Forzar el cambio de estado del vehículo para asegurar que está en estado INCIDENT
        simulation.getState().findVehicleById(vehicleId).setStatus(com.example.plgsystem.enums.VehicleStatus.INCIDENT);
        
        // When - repair vehicle
        boolean repairResult = simulationManager.repairVehicle(simulation.getId(), vehicleId);
        
        // Then
        assertTrue(repairResult, "La reparación del vehículo falló");
    }

    @Test
    public void testShutdown() {
        // Given
        Simulation simulation = simulationManager.createSimulation("Test", "Test Description", LocalDateTime.now());
        simulationManager.startSimulation(simulation.getId());
        
        // When
        simulationManager.shutdown();
        
        // Then
        assertFalse(simulationManager.isSimulationRunning(simulation.getId()));
    }
}
