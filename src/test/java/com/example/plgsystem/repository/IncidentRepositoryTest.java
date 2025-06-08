package com.example.plgsystem.repository;

import com.example.plgsystem.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class IncidentRepositoryTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    public void testIncidentPersistence() {
        // Create a test vehicle first
        Position vehiclePos = new Position(10, 20);
        Vehicle vehicle = new Vehicle("INC-VEH-001", VehicleType.TA, vehiclePos);
        vehicle = vehicleRepository.save(vehicle);

        // Create and save incident
        Incident incident = new Incident();
        incident.setVehicleId(vehicle.getId());
        incident.setTimestamp(LocalDateTime.now());
        incident.setType(IncidentType.TYPE_2);
        incident.setDescription("Engine overheating");
        incident.setResolved(false);
        incident.setPosition(vehiclePos);

        Incident savedIncident = incidentRepository.save(incident);

        // Assert incident was saved correctly
        assertThat(savedIncident.getId()).isNotNull();
        assertThat(savedIncident.getVehicleId()).isEqualTo("INC-VEH-001");
        assertThat(savedIncident.getType()).isEqualTo(IncidentType.TYPE_2);
        assertThat(savedIncident.getDescription()).isEqualTo("Engine overheating");
        assertThat(savedIncident.isResolved()).isFalse();
    }

    @Test
    public void testFindByVehicleId() {
        // Create two test vehicles
        Position pos1 = new Position(10, 20);
        Position pos2 = new Position(30, 40);
        
        Vehicle vehicle1 = new Vehicle("INC-VEH-002", VehicleType.TA, pos1);
        vehicle1 = vehicleRepository.save(vehicle1);
        
        Vehicle vehicle2 = new Vehicle("INC-VEH-003", VehicleType.TA, pos2);
        vehicle2 = vehicleRepository.save(vehicle2);

        // Create incidents for both vehicles
        LocalDateTime now = LocalDateTime.now();
        
        Incident incident1 = new Incident();
        incident1.setVehicleId(vehicle1.getId());
        incident1.setTimestamp(now);
        incident1.setType(IncidentType.TYPE_2);
        incident1.setDescription("Vehicle 1 failure");
        incident1.setResolved(false);
        incident1.setPosition(pos1);
        incidentRepository.save(incident1);
        
        Incident incident2 = new Incident();
        incident2.setVehicleId(vehicle2.getId());
        incident2.setTimestamp(now);
        incident2.setType(IncidentType.TYPE_3);
        incident2.setDescription("Vehicle 2 accident");
        incident2.setResolved(false);
        incident2.setPosition(pos2);
        incidentRepository.save(incident2);
        
        // Test finding incidents by vehicle ID
        List<Incident> vehicle1Incidents = incidentRepository.findByVehicleId(vehicle1.getId());
        
        assertThat(vehicle1Incidents).hasSize(1);
        assertThat(vehicle1Incidents.get(0).getDescription()).isEqualTo("Vehicle 1 failure");
    }

    @Test
    public void testFindByType() {
        // Create test vehicle
        Position pos = new Position(15, 25);
        Vehicle vehicle = new Vehicle("INC-VEH-004", VehicleType.TA, pos);
        vehicle = vehicleRepository.save(vehicle);

        // Create incidents of different types
        LocalDateTime now = LocalDateTime.now();
        
        Incident accident = new Incident();
        accident.setVehicleId(vehicle.getId());
        accident.setTimestamp(now);
        accident.setType(IncidentType.TYPE_3);
        accident.setDescription("Traffic accident");
        accident.setResolved(false);
        accident.setPosition(pos);
        incidentRepository.save(accident);
        
        Incident mechanical = new Incident();
        mechanical.setVehicleId(vehicle.getId());
        mechanical.setTimestamp(now.plusHours(1));
        mechanical.setType(IncidentType.TYPE_2);
        mechanical.setDescription("Engine failure");
        mechanical.setResolved(false);
        mechanical.setPosition(pos);
        incidentRepository.save(mechanical);
        
        // Test finding by type
        List<Incident> accidents = incidentRepository.findByType(IncidentType.TYPE_3);
        
        assertThat(accidents).hasSize(1);
        assertThat(accidents.get(0).getDescription()).isEqualTo("Traffic accident");
    }

    @Test
    public void testFindUnresolvedIncidents() {
        // Create test vehicle
        Position pos = new Position(25, 35);
        Vehicle vehicle = new Vehicle("INC-VEH-005", VehicleType.TA, pos);
        vehicle = vehicleRepository.save(vehicle);
        
        // Create resolved and unresolved incidents
        LocalDateTime now = LocalDateTime.now();
        
        Incident resolved = new Incident();
        resolved.setVehicleId(vehicle.getId());
        resolved.setTimestamp(now.minusHours(2));
        resolved.setType(IncidentType.TYPE_3);
        resolved.setDescription("Resolved incident");
        resolved.setResolved(true);
        resolved.setPosition(pos);
        incidentRepository.save(resolved);
        
        Incident unresolved = new Incident();
        unresolved.setVehicleId(vehicle.getId());
        unresolved.setTimestamp(now);
        unresolved.setType(IncidentType.TYPE_2);
        unresolved.setDescription("Unresolved incident");
        unresolved.setResolved(false);
        unresolved.setPosition(pos);
        incidentRepository.save(unresolved);
        
        // Test finding unresolved incidents
        List<Incident> unresolvedIncidents = incidentRepository.findByResolved(false);
        
        assertThat(unresolvedIncidents).hasSize(1);
        assertThat(unresolvedIncidents.get(0).getDescription()).isEqualTo("Unresolved incident");
    }
}
