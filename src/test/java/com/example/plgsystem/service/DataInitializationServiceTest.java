package com.example.plgsystem.service;

import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.repository.DepotRepository;
import com.example.plgsystem.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataInitializationServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DepotRepository depotRepository;

    @InjectMocks
    private DataInitializationService dataInitializationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void run_shouldInitializeVehiclesWhenRepositoryEmpty() throws Exception {
        // Arrange
        when(vehicleRepository.count()).thenReturn(0L);
        when(depotRepository.count()).thenReturn(1L); // Depots already exist

        // Act
        dataInitializationService.run();

        // Assert
        // Verify that vehicles were initialized for each type
        // Expected count: TA(2) + TB(4) + TC(4) + TD(10) = 20 vehicles
        int expectedVehicleCount = Constants.TA_UNITS + Constants.TB_UNITS + Constants.TC_UNITS + Constants.TD_UNITS;
        verify(vehicleRepository, times(expectedVehicleCount)).save(any(Vehicle.class));
        
        // Verify that depots were not initialized since they already exist
        verify(depotRepository, never()).save(any(Depot.class));
    }

    @Test
    void run_shouldInitializeDepotsWhenRepositoryEmpty() throws Exception {
        // Arrange
        when(vehicleRepository.count()).thenReturn(1L); // Vehicles already exist
        when(depotRepository.count()).thenReturn(0L);

        // Act
        dataInitializationService.run();

        // Assert
        // Verify that vehicles were not initialized since they already exist
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        
        // Verify that depots were initialized (3 depots: central, north, east)
        verify(depotRepository, times(3)).save(any(Depot.class));
    }

    @Test
    void run_shouldNotInitializeWhenRepositoriesNotEmpty() throws Exception {
        // Arrange
        when(vehicleRepository.count()).thenReturn(1L);
        when(depotRepository.count()).thenReturn(1L);

        // Act
        dataInitializationService.run();

        // Assert
        // Verify that no initialization was done
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(depotRepository, never()).save(any(Depot.class));
    }

    @Test
    void initializeVehicles_shouldCreateCorrectVehicleTypesAndIds() throws Exception {
        // Arrange
        when(vehicleRepository.count()).thenReturn(0L);
        when(depotRepository.count()).thenReturn(1L);
        
        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);

        // Act
        dataInitializationService.run();

        // Assert
        verify(vehicleRepository, times(Constants.TA_UNITS + Constants.TB_UNITS + Constants.TC_UNITS + Constants.TD_UNITS))
                .save(vehicleCaptor.capture());
        
        // Check a sample of vehicles to ensure correct types and IDs
        boolean foundTA01 = false;
        boolean foundTB03 = false;
        boolean foundTC02 = false;
        boolean foundTD10 = false;
        
        for (Vehicle vehicle : vehicleCaptor.getAllValues()) {
            if ("TA01".equals(vehicle.getId())) {
                foundTA01 = true;
                assertEquals(VehicleType.TA, vehicle.getType());
            } else if ("TB03".equals(vehicle.getId())) {
                foundTB03 = true;
                assertEquals(VehicleType.TB, vehicle.getType());
            } else if ("TC02".equals(vehicle.getId())) {
                foundTC02 = true;
                assertEquals(VehicleType.TC, vehicle.getType());
            } else if ("TD10".equals(vehicle.getId())) {
                foundTD10 = true;
                assertEquals(VehicleType.TD, vehicle.getType());
            }
        }
        
        assertTrue(foundTA01, "Vehicle TA01 should have been created");
        assertTrue(foundTB03, "Vehicle TB03 should have been created");
        assertTrue(foundTC02, "Vehicle TC02 should have been created");
        assertTrue(foundTD10, "Vehicle TD10 should have been created");
    }

    @Test
    void initializeDepots_shouldCreateCorrectDepotsWithProperties() throws Exception {
        // Arrange
        when(vehicleRepository.count()).thenReturn(1L);
        when(depotRepository.count()).thenReturn(0L);
        
        ArgumentCaptor<Depot> depotCaptor = ArgumentCaptor.forClass(Depot.class);

        // Act
        dataInitializationService.run();

        // Assert
        verify(depotRepository, times(3)).save(depotCaptor.capture());
        
        // Check each depot
        boolean foundCentral = false;
        boolean foundNorth = false;
        boolean foundEast = false;
        
        for (Depot depot : depotCaptor.getAllValues()) {
            if ("CENTRAL".equals(depot.getId())) {
                foundCentral = true;
                assertEquals(1000, depot.getGlpCapacityM3());
                assertTrue(depot.isCanRefuel(), "Central depot should be able to refuel");
                assertEquals(depot.getGlpCapacityM3(), depot.getCurrentGlpM3(), "Depot should be fully filled");
            } else if ("NORTH".equals(depot.getId())) {
                foundNorth = true;
                assertEquals(160, depot.getGlpCapacityM3());
                assertFalse(depot.isCanRefuel(), "North depot should not be able to refuel");
                assertEquals(depot.getGlpCapacityM3(), depot.getCurrentGlpM3(), "Depot should be fully filled");
            } else if ("EAST".equals(depot.getId())) {
                foundEast = true;
                assertEquals(160, depot.getGlpCapacityM3());
                assertFalse(depot.isCanRefuel(), "East depot should not be able to refuel");
                assertEquals(depot.getGlpCapacityM3(), depot.getCurrentGlpM3(), "Depot should be fully filled");
            }
        }
        
        assertTrue(foundCentral, "Central depot should have been created");
        assertTrue(foundNorth, "North depot should have been created");
        assertTrue(foundEast, "East depot should have been created");
    }
} 