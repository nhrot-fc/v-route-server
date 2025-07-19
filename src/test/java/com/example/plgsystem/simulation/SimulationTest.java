package com.example.plgsystem.simulation;

import com.example.plgsystem.model.*;
import com.example.plgsystem.orchest.DatabaseDataLoader;
import com.example.plgsystem.orchest.FileDataLoader;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.SimulationStatus;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.enums.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationTest {

    @Test
    public void testSimulationCreation() {
        // Given
        SimulationState state = createSampleSimulationState(LocalDateTime.now());

        // When
        Simulation simulation = new Simulation(state, SimulationType.CUSTOM, new FileDataLoader());

        // Then
        assertNotNull(simulation.getId());
        assertEquals(state, simulation.getState());
        assertEquals(SimulationStatus.PAUSED, simulation.getStatus());
        assertEquals(SimulationType.CUSTOM, simulation.getType());
    }

    @Test
    public void testSimulationWithSpecifiedType() {
        // Given
        SimulationState state = createSampleSimulationState(LocalDateTime.now());

        // When
        Simulation simulation = new Simulation(state, SimulationType.DAILY_OPERATIONS,
                new DatabaseDataLoader(null, null));

        // Then
        assertEquals(SimulationType.DAILY_OPERATIONS, simulation.getType());
    }

    @Test
    public void testSimulationStatusManagement() {
        // Given
        SimulationState state = createSampleSimulationState(LocalDateTime.now());
        Simulation simulation = new Simulation(state, SimulationType.CUSTOM, new FileDataLoader());

        // When & Then - Test start
        simulation.start();
        assertEquals(SimulationStatus.RUNNING, simulation.getStatus());
        assertTrue(simulation.isRunning());

        // When & Then - Test pause
        simulation.pause();
        assertEquals(SimulationStatus.PAUSED, simulation.getStatus());
        assertTrue(simulation.isPaused());

        // When & Then - Test finish
        simulation.finish();
        assertEquals(SimulationStatus.FINISHED, simulation.getStatus());
        assertTrue(simulation.isFinished());
        assertNotNull(simulation.getRealEndTime());

        // When & Then - Test error
        simulation.error();
        assertEquals(SimulationStatus.ERROR, simulation.getStatus());
        assertTrue(simulation.isError());
    }

    @Test
    public void testDailyOperationSimulation() {
        // Given
        SimulationState state = createSampleSimulationState(LocalDateTime.now());
        Simulation simulation = new Simulation(state, SimulationType.DAILY_OPERATIONS,
                new DatabaseDataLoader(null, null));

        // Start and check that it's running
        simulation.start();
        assertTrue(simulation.isRunning());

        // Daily operations can't be paused
        simulation.pause();
        assertTrue(simulation.isRunning()); // Should still be running
        assertFalse(simulation.isPaused()); // Should not be paused

        // Daily operations can't be finished
        simulation.finish();
        assertTrue(simulation.isRunning()); // Should still be running
        assertFalse(simulation.isFinished()); // Should not be finished

        // But can be set to error
        simulation.error();
        assertTrue(simulation.isError());
    }

    @Test
    public void testDelegatedStateAccess() {
        // Given
        LocalDateTime initialTime = LocalDateTime.of(2025, 7, 5, 10, 0);
        SimulationState state = createSampleSimulationState(initialTime);
        Simulation simulation = new Simulation(state, SimulationType.CUSTOM, new FileDataLoader());

        // Then - Check delegation works for state properties
        assertEquals(initialTime, simulation.getSimulationTime());
        assertEquals(state.getVehicles(), simulation.getState().getVehicles());
        assertEquals(state.getMainDepot(), simulation.getState().getMainDepot());
        assertEquals(state.getAuxDepots(), simulation.getState().getAuxDepots());
    }

    private SimulationState createSampleSimulationState(LocalDateTime time) {
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(new Position(10, 20))
                .build();
        vehicles.add(vehicle);

        Depot mainDepot = new Depot("MD001", new Position(0, 0), 10000, DepotType.MAIN);

        List<Depot> auxDepots = new ArrayList<>();
        auxDepots.add(new Depot("AD001", new Position(50, 50), 2000, DepotType.AUXILIARY));

        return new SimulationState(vehicles, mainDepot, auxDepots, time);
    }
}
