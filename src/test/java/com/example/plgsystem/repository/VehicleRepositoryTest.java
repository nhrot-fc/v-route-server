package com.example.plgsystem.repository;

import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    public void testSaveAndFindById() {
        // Given
        Position position = new Position(10, 20);
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        // When
        vehicleRepository.save(vehicle);
        Optional<Vehicle> foundVehicle = vehicleRepository.findById("V001");

        // Then
        assertTrue(foundVehicle.isPresent());
        assertEquals("V001", foundVehicle.get().getId());
        assertEquals(VehicleType.TA, foundVehicle.get().getType());
        assertEquals(position.getX(), foundVehicle.get().getCurrentPosition().getX());
        assertEquals(position.getY(), foundVehicle.get().getCurrentPosition().getY());
    }

    @Test
    public void testFindByType() {
        // Given
        Position position1 = new Position(10, 20);
        Vehicle vehicle1 = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(position1)
                .build();

        Position position2 = new Position(30, 40);
        Vehicle vehicle2 = Vehicle.builder()
                .id("V002")
                .type(VehicleType.TA)
                .currentPosition(position2)
                .build();

        Position position3 = new Position(50, 60);
        Vehicle vehicle3 = Vehicle.builder()
                .id("V003")
                .type(VehicleType.TB)
                .currentPosition(position3)
                .build();

        vehicleRepository.saveAll(List.of(vehicle1, vehicle2, vehicle3));

        // When
        List<Vehicle> taVehicles = vehicleRepository.findByType(VehicleType.TA);
        List<Vehicle> tbVehicles = vehicleRepository.findByType(VehicleType.TB);

        // Then
        assertEquals(2, taVehicles.size());
        assertEquals(1, tbVehicles.size());
        assertEquals("V003", tbVehicles.get(0).getId());
    }

    @Test
    public void testFindByStatus() {
        // Given
        Position position1 = new Position(10, 20);
        Vehicle vehicle1 = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(position1)
                .build();
        vehicle1.setStatus(VehicleStatus.AVAILABLE);

        Position position2 = new Position(30, 40);
        Vehicle vehicle2 = Vehicle.builder()
                .id("V002")
                .type(VehicleType.TA)
                .currentPosition(position2)
                .build();
        vehicle2.setStatus(VehicleStatus.DRIVING);

        vehicleRepository.saveAll(List.of(vehicle1, vehicle2));

        // When
        List<Vehicle> availableVehicles = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);
        List<Vehicle> inRouteVehicles = vehicleRepository.findByStatus(VehicleStatus.DRIVING);

        // Then
        assertEquals(1, availableVehicles.size());
        assertEquals(1, inRouteVehicles.size());
        assertEquals("V001", availableVehicles.get(0).getId());
        assertEquals("V002", inRouteVehicles.get(0).getId());
    }

    @Test
    public void testDeleteById() {
        // Given
        Position position = new Position(10, 20);
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        vehicleRepository.save(vehicle);

        // When
        vehicleRepository.deleteById("V001");
        Optional<Vehicle> foundVehicle = vehicleRepository.findById("V001");

        // Then
        assertFalse(foundVehicle.isPresent());
    }
}
