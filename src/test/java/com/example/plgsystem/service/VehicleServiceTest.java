package com.example.plgsystem.service;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.ServeRecord;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class VehicleServiceTest {

	@InjectMocks
	private VehicleService vehicleService;

	@Mock
	private VehicleRepository vehicleRepository;

	@Mock
	private OrderService orderService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void save_shouldReturnSavedVehicle() {
		// Arrange
		Position position = new Position(10, 10);
		Vehicle vehicle = Vehicle.builder()
				.id("V-001")
				.type(VehicleType.TA)
				.currentPosition(position)
				.build();

		when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

		// Act
		Vehicle savedVehicle = vehicleService.save(vehicle);

		// Assert
		assertEquals("V-001", savedVehicle.getId());
		assertEquals(VehicleType.TA, savedVehicle.getType());
		assertEquals(position, savedVehicle.getCurrentPosition());
		verify(vehicleRepository, times(1)).save(vehicle);
	}

	@Test
	void findById_shouldReturnVehicleWhenExists() {
		// Arrange
		Position position = new Position(10, 10);
		Vehicle vehicle = Vehicle.builder()
				.id("V-001")
				.type(VehicleType.TA)
				.currentPosition(position)
				.build();

		when(vehicleRepository.findById("V-001")).thenReturn(Optional.of(vehicle));

		// Act
		Optional<Vehicle> result = vehicleService.findById("V-001");

		// Assert
		assertTrue(result.isPresent());
		assertEquals("V-001", result.get().getId());
	}

	@Test
	void findById_shouldReturnEmptyWhenNotExists() {
		// Arrange
		when(vehicleRepository.findById("non-existent")).thenReturn(Optional.empty());

		// Act
		Optional<Vehicle> result = vehicleService.findById("non-existent");

		// Assert
		assertFalse(result.isPresent());
	}

	@Test
	void findAll_shouldReturnAllVehicles() {
		// Arrange
		Position position1 = new Position(10, 10);
		Position position2 = new Position(20, 20);

		Vehicle vehicle1 = Vehicle.builder()
				.id("V-001")
				.type(VehicleType.TA)
				.currentPosition(position1)
				.build();

		Vehicle vehicle2 = Vehicle.builder()
				.id("V-002")
				.type(VehicleType.TB)
				.currentPosition(position2)
				.build();

		List<Vehicle> vehicles = new ArrayList<>();
		vehicles.add(vehicle1);
		vehicles.add(vehicle2);

		when(vehicleRepository.findAll()).thenReturn(vehicles);

		// Act
		List<Vehicle> result = vehicleService.findAll();

		// Assert
		assertEquals(2, result.size());
		assertEquals("V-001", result.get(0).getId());
		assertEquals("V-002", result.get(1).getId());
	}

	@Test
	void findAllPaged_shouldReturnPagedVehicles() {
		// Arrange
		Position position1 = new Position(10, 10);
		Position position2 = new Position(20, 20);

		Vehicle vehicle1 = Vehicle.builder()
				.id("V-001")
				.type(VehicleType.TA)
				.currentPosition(position1)
				.build();

		Vehicle vehicle2 = Vehicle.builder()
				.id("V-002")
				.type(VehicleType.TB)
				.currentPosition(position2)
				.build();

		List<Vehicle> vehicles = new ArrayList<>();
		vehicles.add(vehicle1);
		vehicles.add(vehicle2);

		Page<Vehicle> pagedVehicles = new PageImpl<>(vehicles);
		Pageable pageable = PageRequest.of(0, 10);
		when(vehicleRepository.findAll(pageable)).thenReturn(pagedVehicles);

		// Act
		Page<Vehicle> result = vehicleService.findAllPaged(pageable);

		// Assert
		assertEquals(2, result.getContent().size());
	}

	@Test
	void deleteById_shouldCallRepositoryDelete() {
		// Arrange
		String vehicleId = "V-001";
		doNothing().when(vehicleRepository).deleteById(vehicleId);

		// Act
		vehicleService.deleteById(vehicleId);

		// Assert
		verify(vehicleRepository, times(1)).deleteById(vehicleId);
	}

	@Test
	void findByType_shouldReturnVehiclesOfType() {
		// Arrange
		Position position1 = new Position(10, 10);
		Position position2 = new Position(20, 20);

		Vehicle vehicle1 = Vehicle.builder()
				.id("V-001")
				.type(VehicleType.TA)
				.currentPosition(position1)
				.build();

		Vehicle vehicle2 = Vehicle.builder()
				.id("V-002")
				.type(VehicleType.TA)
				.currentPosition(position2)
				.build();

		List<Vehicle> taVehicles = new ArrayList<>();
		taVehicles.add(vehicle1);
		taVehicles.add(vehicle2);

		when(vehicleRepository.findByType(VehicleType.TA)).thenReturn(taVehicles);

		// Act
		List<Vehicle> result = vehicleService.findByType(VehicleType.TA);

		// Assert
		assertEquals(2, result.size());
		assertEquals(VehicleType.TA, result.get(0).getType());
		assertEquals(VehicleType.TA, result.get(1).getType());
	}

	@Test
	void findByStatus_shouldReturnVehiclesWithStatus() {
		// Arrange
		Position position1 = new Position(10, 10);
		Position position2 = new Position(20, 20);

		Vehicle vehicle1 = Vehicle.builder()
				.id("V-001")
				.type(VehicleType.TA)
				.currentPosition(position1)
				.build();

		Vehicle vehicle2 = Vehicle.builder()
				.id("V-002")
				.type(VehicleType.TB)
				.currentPosition(position2)
				.build();

		vehicle1.setAvailable();
		vehicle2.setAvailable();

		List<Vehicle> availableVehicles = new ArrayList<>();
		availableVehicles.add(vehicle1);
		availableVehicles.add(vehicle2);

		when(vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)).thenReturn(availableVehicles);

		// Act
		List<Vehicle> result = vehicleService.findByStatus(VehicleStatus.AVAILABLE);

		// Assert
		assertEquals(2, result.size());
		assertEquals(VehicleStatus.AVAILABLE, result.get(0).getStatus());
		assertEquals(VehicleStatus.AVAILABLE, result.get(1).getStatus());
	}

	@Test
	void refuelVehicle_shouldRefillFuelTankToMax() {
		// Arrange
		Position position = new Position(10, 10);
		Vehicle vehicle = Vehicle.builder()
				.id("V-001")
				.type(VehicleType.TA)
				.currentPosition(position)
				.build();

		double initialCapacity = vehicle.getCurrentFuelGal();
		vehicle.consumeFuel(initialCapacity * 0.75); // Consume 75% of fuel

		when(vehicleRepository.findById("V-001")).thenReturn(Optional.of(vehicle));
		when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArguments()[0]);

		// Act
		Optional<Vehicle> result = vehicleService.refuelVehicle("V-001");

		// Assert
		assertTrue(result.isPresent());
		assertEquals(initialCapacity, result.get().getCurrentFuelGal()); // Should be full again
		verify(vehicleRepository, times(1)).save(vehicle);
	}

	@Test
	void refillGlp_shouldRefillGlpTank() {
		// Arrange
		Position position = new Position(10, 10);
		Vehicle vehicle = Vehicle.builder()
				.id("V-001")
				.type(VehicleType.TA)
				.currentPosition(position)
				.build();

		// Verify initial GLP is 0
		assertEquals(0, vehicle.getCurrentGlpM3());

		int glpToAdd = 15;

		// Mock the repository to return the same vehicle that was passed to it
		when(vehicleRepository.findById("V-001")).thenReturn(Optional.of(vehicle));
		when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
			return invocation.getArgument(0);
		});

		// Act
		Optional<Vehicle> result = vehicleService.refillGlp("V-001", glpToAdd);

		// Assert
		assertTrue(result.isPresent());
		assertEquals(glpToAdd, result.get().getCurrentGlpM3());
		verify(vehicleRepository).save(vehicle);
	}

	@Test
	void dispenseGlp_shouldReduceGlpLevel() {
		// Arrange
		Position position = new Position(10, 10);
		Vehicle vehicle = Vehicle.builder()
				.id("V-001")
				.type(VehicleType.TA)
				.currentPosition(position)
				.build();

		// Add GLP first
		int initialGlp = 40;
		vehicle.setCurrentGlpM3(initialGlp);

		// Verify GLP was set correctly
		assertEquals(initialGlp, vehicle.getCurrentGlpM3());

		int glpToDispense = 15;
		int expectedRemaining = initialGlp - glpToDispense;

		// Mock repository to return the same vehicle that was passed to it
		when(vehicleRepository.findById("V-001")).thenReturn(Optional.of(vehicle));
		when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
			return invocation.getArgument(0);
		});

		// Act
		Optional<Vehicle> result = vehicleService.dispenseGlp("V-001", glpToDispense);

		// Assert
		assertTrue(result.isPresent());
		assertEquals(expectedRemaining, result.get().getCurrentGlpM3());
		verify(vehicleRepository).save(vehicle);
	}

	@Test
	void serveOrder_shouldCreateServeRecordAndUpdateVehicleAndOrder() {
		// Arrange
		Position position = new Position(10, 10);

		// Create and set up the vehicle
		Vehicle vehicle = Vehicle.builder()
				.id("V-001")
				.type(VehicleType.TA)
				.currentPosition(position)
				.build();

		int initialGlp = 40;
		vehicle.setCurrentGlpM3(initialGlp);

		// Create and set up the order
		Order order = Order.builder()
				.id("O-001")
				.arrivalTime(LocalDateTime.now().minusHours(1))
				.deadlineTime(LocalDateTime.now().plusHours(2))
				.glpRequestM3(20)
				.position(position)
				.build();
		assertEquals(20, order.getRemainingGlpM3());

		// Set delivery amount
		int deliveryAmount = 15;
		LocalDateTime serveTime = LocalDateTime.now();

		// Expected values after delivery
		int expectedVehicleGlp = initialGlp - deliveryAmount;
		int expectedOrderRemainingGlp = order.getGlpRequestM3() - deliveryAmount;

		// Mock the repositories
		when(vehicleRepository.findById("V-001")).thenReturn(Optional.of(vehicle));
		when(orderService.findById("O-001")).thenReturn(Optional.of(order));

		// Mock save methods to return the same objects that were passed to them
		when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
			return invocation.getArgument(0);
		});

		when(orderService.save(any(Order.class))).thenAnswer(invocation -> {
			return invocation.getArgument(0);
		});

		// Act
		Optional<ServeRecord> result = vehicleService.serveOrder("V-001", "O-001", deliveryAmount, serveTime);

		// Assert
		assertTrue(result.isPresent());
		ServeRecord serveRecord = result.get();

		// Verify ServeRecord details
		assertEquals("V-001", serveRecord.getVehicle().getId());
		assertEquals("O-001", serveRecord.getOrder().getId());
		assertEquals(deliveryAmount, serveRecord.getGlpVolumeM3());
		assertEquals(serveTime, serveRecord.getServeDate());

		// Verify Vehicle and Order were updated correctly
		assertEquals(expectedVehicleGlp, vehicle.getCurrentGlpM3());
		assertEquals(expectedOrderRemainingGlp, order.getRemainingGlpM3());

		// Verify repository calls
		verify(vehicleRepository).save(vehicle);
		verify(orderService).save(order);
	}

	@Test
	void moveVehicle_shouldUpdatePositionAndConsumeFuel() {
		// Arrange
		Position position = new Position(10, 10);
		Vehicle vehicle = Vehicle.builder()
				.id("V-001")
				.type(VehicleType.TA)
				.currentPosition(position)
				.build();

		double initialFuel = vehicle.getCurrentFuelGal();
		double distanceKm = 100;

		when(vehicleRepository.findById("V-001")).thenReturn(Optional.of(vehicle));
		when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArguments()[0]);

		// Act
		Optional<Vehicle> result = vehicleService.moveVehicle("V-001", distanceKm);

		// Assert
		assertTrue(result.isPresent());
		assertTrue(result.get().getCurrentFuelGal() < initialFuel); // Some fuel should be consumed
		verify(vehicleRepository, times(1)).save(vehicle);
	}
}