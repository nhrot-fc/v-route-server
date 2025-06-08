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
public class MaintenanceRepositoryTest {

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    public void testMaintenancePersistence() {
        // Create a test vehicle first
        Position pos = new Position(10, 20);
        Vehicle vehicle = new Vehicle("MAINT-VEH-001", VehicleType.TA, pos);
        vehicle = vehicleRepository.save(vehicle);

        // Create and save maintenance
        LocalDateTime now = LocalDateTime.now();
        Maintenance maintenance = new Maintenance();
        maintenance.setVehicleId(vehicle.getId());
        maintenance.setStartDate(now);
        maintenance.setEndDate(now.plusHours(4));
        maintenance.setType(MaintenanceType.PREVENTIVE);
        maintenance.setDescription("Regular check-up");
        maintenance.setCompleted(false);
        maintenance.setScheduledDate(now.plusDays(1));

        Maintenance savedMaintenance = maintenanceRepository.save(maintenance);

        // Assert maintenance was saved correctly
        assertThat(savedMaintenance.getId()).isNotNull();
        assertThat(savedMaintenance.getVehicleId()).isEqualTo("MAINT-VEH-001");
        assertThat(savedMaintenance.getType()).isEqualTo(MaintenanceType.PREVENTIVE);
        assertThat(savedMaintenance.getDescription()).isEqualTo("Regular check-up");
        assertThat(savedMaintenance.isCompleted()).isFalse();
    }

    @Test
    public void testFindByVehicleId() {
        // Create two test vehicles
        Position pos1 = new Position(10, 20);
        Position pos2 = new Position(30, 40);
        
        Vehicle vehicle1 = new Vehicle("MAINT-VEH-002", VehicleType.TA, pos1);
        vehicle1 = vehicleRepository.save(vehicle1);
        
        Vehicle vehicle2 = new Vehicle("MAINT-VEH-003", VehicleType.TA, pos2);
        vehicle2 = vehicleRepository.save(vehicle2);

        // Create maintenance records for both vehicles
        LocalDateTime now = LocalDateTime.now();
        
        Maintenance maintenance1 = new Maintenance();
        maintenance1.setVehicleId(vehicle1.getId());
        maintenance1.setStartDate(now);
        maintenance1.setEndDate(now.plusHours(2));
        maintenance1.setType(MaintenanceType.PREVENTIVE);
        maintenance1.setDescription("Vehicle 1 maintenance");
        maintenance1.setCompleted(false);
        maintenanceRepository.save(maintenance1);
        
        Maintenance maintenance2 = new Maintenance();
        maintenance2.setVehicleId(vehicle2.getId());
        maintenance2.setStartDate(now.plusHours(1));
        maintenance2.setEndDate(now.plusHours(3));
        maintenance2.setType(MaintenanceType.CORRECTIVE);
        maintenance2.setDescription("Vehicle 2 maintenance");
        maintenance2.setCompleted(false);
        maintenanceRepository.save(maintenance2);
        
        // Test finding maintenance by vehicle ID
        List<Maintenance> vehicle1Maintenance = maintenanceRepository.findByVehicleId(vehicle1.getId());
        
        assertThat(vehicle1Maintenance).hasSize(1);
        assertThat(vehicle1Maintenance.get(0).getDescription()).isEqualTo("Vehicle 1 maintenance");
    }

    @Test
    public void testFindByType() {
        // Create test vehicle
        Position pos = new Position(15, 25);
        Vehicle vehicle = new Vehicle("MAINT-VEH-004", VehicleType.TA, pos);
        vehicle = vehicleRepository.save(vehicle);

        // Create maintenance records of different types
        LocalDateTime now = LocalDateTime.now();
        
        Maintenance preventive = new Maintenance();
        preventive.setVehicleId(vehicle.getId());
        preventive.setStartDate(now);
        preventive.setEndDate(now.plusHours(2));
        preventive.setType(MaintenanceType.PREVENTIVE);
        preventive.setDescription("Preventive maintenance");
        preventive.setCompleted(false);
        maintenanceRepository.save(preventive);
        
        Maintenance corrective = new Maintenance();
        corrective.setVehicleId(vehicle.getId());
        corrective.setStartDate(now.plusDays(1));
        corrective.setEndDate(now.plusDays(1).plusHours(4));
        corrective.setType(MaintenanceType.CORRECTIVE);
        corrective.setDescription("Corrective maintenance");
        corrective.setCompleted(false);
        maintenanceRepository.save(corrective);
        
        // Test finding by type
        List<Maintenance> preventiveMaintenances = maintenanceRepository.findByType(MaintenanceType.PREVENTIVE);
        
        assertThat(preventiveMaintenances).hasSize(1);
        assertThat(preventiveMaintenances.get(0).getDescription()).isEqualTo("Preventive maintenance");
    }

    @Test
    public void testFindActiveMaintenance() {
        // Create test vehicle
        Position pos = new Position(25, 35);
        Vehicle vehicle = new Vehicle("MAINT-VEH-005", VehicleType.TA, pos);
        vehicle = vehicleRepository.save(vehicle);
        
        LocalDateTime now = LocalDateTime.now();
        
        // Create active maintenance (current time is between start and end)
        Maintenance active = new Maintenance();
        active.setVehicleId(vehicle.getId());
        active.setStartDate(now.minusHours(1));
        active.setEndDate(now.plusHours(1));
        active.setType(MaintenanceType.PREVENTIVE);
        active.setDescription("Active maintenance");
        active.setCompleted(false);
        maintenanceRepository.save(active);
        
        // Create past maintenance
        Maintenance past = new Maintenance();
        past.setVehicleId(vehicle.getId());
        past.setStartDate(now.minusHours(3));
        past.setEndDate(now.minusHours(2));
        past.setType(MaintenanceType.CORRECTIVE);
        past.setDescription("Past maintenance");
        past.setCompleted(true);
        maintenanceRepository.save(past);
        
        // Create future maintenance
        Maintenance future = new Maintenance();
        future.setVehicleId(vehicle.getId());
        future.setStartDate(now.plusHours(2));
        future.setEndDate(now.plusHours(4));
        future.setType(MaintenanceType.PREVENTIVE);
        future.setDescription("Future maintenance");
        future.setCompleted(false);
        maintenanceRepository.save(future);
        
        // Test finding active maintenance
        List<Maintenance> activeMaintenances = maintenanceRepository.findActiveMaintenance(now);
        
        assertThat(activeMaintenances).hasSize(1);
        assertThat(activeMaintenances.get(0).getDescription()).isEqualTo("Active maintenance");
    }

    @Test
    public void testFindActiveMaintenanceForVehicle() {
        // Create two test vehicles
        Position pos1 = new Position(10, 20);
        Position pos2 = new Position(30, 40);
        
        Vehicle vehicle1 = new Vehicle("MAINT-VEH-006", VehicleType.TA, pos1);
        vehicle1 = vehicleRepository.save(vehicle1);
        
        Vehicle vehicle2 = new Vehicle("MAINT-VEH-007", VehicleType.TA, pos2);
        vehicle2 = vehicleRepository.save(vehicle2);
        
        LocalDateTime now = LocalDateTime.now();
        
        // Create active maintenance for vehicle 1
        Maintenance active1 = new Maintenance();
        active1.setVehicleId(vehicle1.getId());
        active1.setStartDate(now.minusHours(1));
        active1.setEndDate(now.plusHours(1));
        active1.setType(MaintenanceType.PREVENTIVE);
        active1.setDescription("Vehicle 1 active maintenance");
        active1.setCompleted(false);
        maintenanceRepository.save(active1);
        
        // Create active maintenance for vehicle 2
        Maintenance active2 = new Maintenance();
        active2.setVehicleId(vehicle2.getId());
        active2.setStartDate(now.minusHours(1));
        active2.setEndDate(now.plusHours(1));
        active2.setType(MaintenanceType.CORRECTIVE);
        active2.setDescription("Vehicle 2 active maintenance");
        active2.setCompleted(false);
        maintenanceRepository.save(active2);
        
        // Test finding active maintenance for vehicle 1
        List<Maintenance> activeMaintenancesVehicle1 = maintenanceRepository.findActiveMaintenanceForVehicle(vehicle1.getId(), now);
        
        assertThat(activeMaintenancesVehicle1).hasSize(1);
        assertThat(activeMaintenancesVehicle1.get(0).getDescription()).isEqualTo("Vehicle 1 active maintenance");
    }

    @Test
    public void testFindUpcomingMaintenance() {
        // Create test vehicle
        Position pos = new Position(35, 45);
        Vehicle vehicle = new Vehicle("MAINT-VEH-008", VehicleType.TA, pos);
        vehicle = vehicleRepository.save(vehicle);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);
        LocalDateTime nextWeek = now.plusDays(7);
        
        // Create maintenance scheduled for tomorrow
        Maintenance tomorrowMaintenance = new Maintenance();
        tomorrowMaintenance.setVehicleId(vehicle.getId());
        tomorrowMaintenance.setStartDate(tomorrow);
        tomorrowMaintenance.setEndDate(tomorrow.plusHours(2));
        tomorrowMaintenance.setType(MaintenanceType.PREVENTIVE);
        tomorrowMaintenance.setDescription("Tomorrow maintenance");
        tomorrowMaintenance.setCompleted(false);
        maintenanceRepository.save(tomorrowMaintenance);
        
        // Create maintenance scheduled for next week
        Maintenance nextWeekMaintenance = new Maintenance();
        nextWeekMaintenance.setVehicleId(vehicle.getId());
        nextWeekMaintenance.setStartDate(nextWeek);
        nextWeekMaintenance.setEndDate(nextWeek.plusHours(3));
        nextWeekMaintenance.setType(MaintenanceType.PREVENTIVE);
        nextWeekMaintenance.setDescription("Next week maintenance");
        nextWeekMaintenance.setCompleted(false);
        maintenanceRepository.save(nextWeekMaintenance);
        
        // Create maintenance scheduled for today
        Maintenance todayMaintenance = new Maintenance();
        todayMaintenance.setVehicleId(vehicle.getId());
        todayMaintenance.setStartDate(now.plusHours(2));
        todayMaintenance.setEndDate(now.plusHours(4));
        todayMaintenance.setType(MaintenanceType.CORRECTIVE);
        todayMaintenance.setDescription("Today maintenance");
        todayMaintenance.setCompleted(false);
        maintenanceRepository.save(todayMaintenance);
        
        // Test finding upcoming maintenance for the next 2 days only
        List<Maintenance> upcomingMaintenance = maintenanceRepository.findUpcomingMaintenance(now, now.plusDays(2));
        
        assertThat(upcomingMaintenance).hasSize(2);
        // Check that it contains the today and tomorrow maintenance but not the next week one
        boolean hasTodayMaintenance = upcomingMaintenance.stream().anyMatch(m -> m.getDescription().equals("Today maintenance"));
        boolean hasTomorrowMaintenance = upcomingMaintenance.stream().anyMatch(m -> m.getDescription().equals("Tomorrow maintenance"));
        
        assertThat(hasTodayMaintenance).isTrue();
        assertThat(hasTomorrowMaintenance).isTrue();
    }
}
