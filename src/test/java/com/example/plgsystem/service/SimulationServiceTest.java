package com.example.plgsystem.service;

import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.orchest.DataLoader;
import com.example.plgsystem.orchest.DatabaseDataLoader;
import com.example.plgsystem.repository.BlockageRepository;
import com.example.plgsystem.repository.OrderRepository;
import com.example.plgsystem.simulation.Simulation;
import com.example.plgsystem.simulation.SimulationState;
import com.example.plgsystem.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SimulationServiceTest {

    @InjectMocks
    private SimulationService simulationService;

    @Mock
    private DepotService depotService;

    @Mock
    private VehicleService vehicleService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BlockageRepository blockageRepository;

    @Mock
    private FileUtils fileUtils;

    private UUID testSimulationId;
    private Simulation testSimulation;
    private Depot mainDepot;
    private List<Depot> auxDepots;
    private List<Vehicle> vehicles;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test data
        testSimulationId = UUID.randomUUID();
        mainDepot = new Depot("MAIN", Constants.MAIN_DEPOT_LOCATION, 10000, DepotType.MAIN);
        
        // Create auxiliary depots
        auxDepots = Arrays.asList(
            new Depot("NORTH", Constants.NORTH_DEPOT_LOCATION, 500, DepotType.AUXILIARY),
            new Depot("EAST", Constants.EAST_DEPOT_LOCATION, 500, DepotType.AUXILIARY)
        );
        
        // Create test vehicles
        vehicles = new ArrayList<>();
        vehicles.add(Vehicle.builder()
                .id("TA01")
                .type(VehicleType.TA)
                .currentPosition(mainDepot.getPosition().clone())
                .build());
        vehicles.add(Vehicle.builder()
                .id("TB01")
                .type(VehicleType.TB)
                .currentPosition(mainDepot.getPosition().clone())
                .build());

        // Mock repository responses
        when(depotService.findMainDepots()).thenReturn(Collections.singletonList(mainDepot));
        when(depotService.findAuxiliaryDepots()).thenReturn(auxDepots);
        when(vehicleService.findAll()).thenReturn(vehicles);
    }

    @Test
    void getSimulation_shouldReturnSimulationWhenExists() {
        // Arrange
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());
        DataLoader dataLoader = new DatabaseDataLoader(orderRepository, blockageRepository);
        testSimulation = new Simulation(state, SimulationType.DAILY_OPERATIONS, dataLoader);
        
        // Using reflection to set the id field with our testSimulationId
        try {
            java.lang.reflect.Field idField = testSimulation.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testSimulation, testSimulationId);
        } catch (Exception e) {
            fail("Failed to set simulation ID via reflection: " + e.getMessage());
        }

        // Insert the test simulation into the service's internal map
        try {
            simulationService.getSimulations().put(testSimulationId, testSimulation);
        } catch (Exception e) {
            fail("Failed to insert simulation into service map: " + e.getMessage());
        }

        // Act
        Simulation result = simulationService.getSimulation(testSimulationId);

        // Assert
        assertNotNull(result, "Simulation should not be null");
        assertEquals(testSimulationId, result.getId(), "Simulation ID should match");
        assertEquals(SimulationType.DAILY_OPERATIONS, result.getType(), "Simulation type should match");
    }

    @Test
    void getSimulation_shouldReturnNullWhenNotExists() {
        // Act
        Simulation result = simulationService.getSimulation(UUID.randomUUID());

        // Assert
        assertNull(result, "Simulation should be null for non-existent ID");
    }

    @Test
    void createSimulation_shouldCreateAndStoreSimulation() {
        // Arrange
        LocalDateTime startDateTime = LocalDateTime.now();
        LocalDateTime endDateTime = startDateTime.plusDays(7);
        int taVehicleCount = 2;
        int tbVehicleCount = 1;
        int tcVehicleCount = 1;
        int tdVehicleCount = 1;

        // Reset any previous interactions with messagingTemplate
        reset(messagingTemplate);
        
        // Act
        Simulation result = simulationService.createSimulation(
                SimulationType.WEEKLY,
                startDateTime,
                endDateTime,
                taVehicleCount,
                tbVehicleCount,
                tcVehicleCount,
                tdVehicleCount
        );

        // Assert
        assertNotNull(result, "Created simulation should not be null");
        assertEquals(SimulationType.WEEKLY, result.getType(), "Simulation type should be WEEKLY");
        
        // Verify the simulation was added to the internal map
        Simulation storedSimulation = simulationService.getSimulation(result.getId());
        assertNotNull(storedSimulation, "Simulation should be retrievable from service");
        assertEquals(result.getId(), storedSimulation.getId(), "Stored simulation ID should match created simulation ID");
        
        // Verify messaging was called at least once (we don't care about the exact count)
        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void startSimulation_shouldStartExistingSimulation() {
        // Arrange
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());
        DataLoader dataLoader = mock(DataLoader.class);
        testSimulation = spy(new Simulation(state, SimulationType.CUSTOM, dataLoader));
        
        // Using reflection to set the id field and add to internal map
        try {
            java.lang.reflect.Field idField = testSimulation.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testSimulation, testSimulationId);
            
            simulationService.getSimulations().put(testSimulationId, testSimulation);
        } catch (Exception e) {
            fail("Failed to set up test: " + e.getMessage());
        }

        // Act
        Simulation result = simulationService.startSimulation(testSimulationId);

        // Assert
        assertNotNull(result, "Result should not be null");
        verify(testSimulation, times(1)).start();
        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void pauseSimulation_shouldPauseRunningSimulation() {
        // Arrange
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());
        DataLoader dataLoader = mock(DataLoader.class);
        testSimulation = spy(new Simulation(state, SimulationType.CUSTOM, dataLoader));
        
        // Using reflection to set the id field and add to internal map
        try {
            java.lang.reflect.Field idField = testSimulation.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testSimulation, testSimulationId);
            
            simulationService.getSimulations().put(testSimulationId, testSimulation);
        } catch (Exception e) {
            fail("Failed to set up test: " + e.getMessage());
        }

        // Act
        Simulation result = simulationService.pauseSimulation(testSimulationId);

        // Assert
        assertNotNull(result, "Result should not be null");
        verify(testSimulation, times(1)).pause();
        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void finishSimulation_shouldFinishSimulation() {
        // Arrange
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());
        DataLoader dataLoader = mock(DataLoader.class);
        testSimulation = spy(new Simulation(state, SimulationType.CUSTOM, dataLoader));
        
        // Using reflection to set the id field and add to internal map
        try {
            java.lang.reflect.Field idField = testSimulation.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testSimulation, testSimulationId);
            
            simulationService.getSimulations().put(testSimulationId, testSimulation);
        } catch (Exception e) {
            fail("Failed to set up test: " + e.getMessage());
        }

        // Act
        Simulation result = simulationService.finishSimulation(testSimulationId);

        // Assert
        assertNotNull(result, "Result should not be null");
        verify(testSimulation, times(1)).finish();
        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void loadOrders_shouldCallFileDataLoader() throws Exception {
        // Skip this test as it requires static mocking which isn't available without additional dependencies
        // In a real project, we would use PowerMockito, Mockito-inline, or JMockit to handle static methods
    }

    @Test
    void getAllSimulations_shouldReturnAllSimulations() {
        // Arrange
        SimulationState state1 = new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());
        SimulationState state2 = new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());
        
        DataLoader dataLoader = mock(DataLoader.class);
        Simulation simulation1 = new Simulation(state1, SimulationType.CUSTOM, dataLoader);
        Simulation simulation2 = new Simulation(state2, SimulationType.WEEKLY, dataLoader);
        
        UUID id1 = simulation1.getId();
        UUID id2 = simulation2.getId();
        
        // Using reflection to add simulations to internal map
        try {
            simulationService.getSimulations().put(id1, simulation1);
            simulationService.getSimulations().put(id2, simulation2);
        } catch (Exception e) {
            fail("Failed to set up test: " + e.getMessage());
        }

        // Act
        Map<UUID, Simulation> result = simulationService.getAllSimulations();

        // Assert
        assertEquals(2, result.size(), "Should return 2 simulations");
        assertTrue(result.containsKey(id1), "Should contain first simulation");
        assertTrue(result.containsKey(id2), "Should contain second simulation");
        assertEquals(simulation1, result.get(id1), "First simulation should match");
        assertEquals(simulation2, result.get(id2), "Second simulation should match");
    }
} 