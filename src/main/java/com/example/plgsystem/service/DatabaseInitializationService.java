package com.example.plgsystem.service;

import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.repository.DepotRepository;
import com.example.plgsystem.repository.MaintenanceRepository;
import com.example.plgsystem.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for initializing the database with required entities
 * (depots, vehicles, maintenance plans) during application startup.
 */
@Service
public class DatabaseInitializationService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializationService.class);

    private final DepotRepository depotRepository;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceRepository maintenanceRepository;

    public DatabaseInitializationService(
            DepotRepository depotRepository,
            VehicleRepository vehicleRepository,
            MaintenanceRepository maintenanceRepository) {
        this.depotRepository = depotRepository;
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    /**
     * Initializes the database with required entities.
     * Should be called during application startup.
     */
    @Transactional
    public void initializeDatabase() {
        logger.info("Starting database initialization");

        // Initialize in correct order to maintain dependencies
        initializeDepots();
        initializeVehicles();
        initializeMaintenancePlans();

        logger.info("Database initialization completed");
    }

    /**
     * Initializes the depots if they don't exist in the database.
     */
    @Transactional
    public void initializeDepots() {
        logger.info("Initializing depots");

        long depotCount = depotRepository.count();
        if (depotCount > 0) {
            logger.info("Depots already initialized, found {} depots", depotCount);
            return;
        }

        logger.info("No depots found in the database, creating default depots");
        List<Depot> depots = new ArrayList<>();

        // Create main depot
        Depot mainDepot = new Depot("MAIN", Constants.MAIN_DEPOT_LOCATION, 10000, DepotType.MAIN);
        depots.add(mainDepot);

        // Create auxiliary depots
        Depot northDepot = new Depot("NORTH", Constants.NORTH_DEPOT_LOCATION, 160, DepotType.AUXILIARY);
        Depot eastDepot = new Depot("EAST", Constants.EAST_DEPOT_LOCATION, 160, DepotType.AUXILIARY);
        depots.add(northDepot);
        depots.add(eastDepot);

        // Save all depots in a single operation
        depotRepository.saveAll(depots);
        logger.info("Created {} depots", depots.size());
    }

    /**
     * Initializes the vehicles if they don't exist in the database.
     */
    @Transactional
    public void initializeVehicles() {
        logger.info("Initializing vehicles");

        long vehicleCount = vehicleRepository.count();
        if (vehicleCount > 0) {
            logger.info("Vehicles already initialized, found {} vehicles", vehicleCount);
            return;
        }

        logger.info("No vehicles found in the database, creating default fleet");

        // Get the main depot reference for vehicle positioning
        Depot mainDepot = depotRepository.findByType(DepotType.MAIN)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Main depot not found"));

        Position depotPosition = mainDepot.getPosition().clone();
        List<Vehicle> vehicles = new ArrayList<>();

        // Create TA vehicles (25m³, 2 units)
        for (int i = 0; i < Constants.TA_UNITS; i++) {
            String id = "TA" + String.format("%02d", i + 1);
            Vehicle vehicle = Vehicle.builder()
                    .id(id)
                    .type(VehicleType.TA)
                    .currentPosition(depotPosition.clone())
                    .build();
            vehicles.add(vehicle);
        }

        // Create TB vehicles (15m³, 4 units)
        for (int i = 0; i < Constants.TB_UNITS; i++) {
            String id = "TB" + String.format("%02d", i + 1);
            Vehicle vehicle = Vehicle.builder()
                    .id(id)
                    .type(VehicleType.TB)
                    .currentPosition(depotPosition.clone())
                    .build();
            vehicles.add(vehicle);
        }

        // Create TC vehicles (10m³, 4 units)
        for (int i = 0; i < Constants.TC_UNITS; i++) {
            String id = "TC" + String.format("%02d", i + 1);
            Vehicle vehicle = Vehicle.builder()
                    .id(id)
                    .type(VehicleType.TC)
                    .currentPosition(depotPosition.clone())
                    .build();
            vehicles.add(vehicle);
        }

        // Create TD vehicles (5m³, 10 units)
        for (int i = 0; i < Constants.TD_UNITS; i++) {
            String id = "TD" + String.format("%02d", i + 1);
            Vehicle vehicle = Vehicle.builder()
                    .id(id)
                    .type(VehicleType.TD)
                    .currentPosition(depotPosition.clone())
                    .build();
            vehicles.add(vehicle);
        }

        // Save all vehicles in a single operation
        vehicleRepository.saveAll(vehicles);
        logger.info("Created {} vehicles in the fleet", vehicles.size());
    }

    /**
     * Initializes maintenance plans if they don't exist in the database.
     */
    @Transactional
    public void initializeMaintenancePlans() {
        logger.info("Initializing maintenance plans");

        // Check if maintenance plans already exist for the standard period
        LocalDate startDate = LocalDate.of(2025, 4, 1);
        LocalDate endDate = LocalDate.of(2025, 5, 31);
        List<Maintenance> existingPlans = maintenanceRepository.findByAssignedDateBetween(startDate, endDate);

        if (!existingPlans.isEmpty()) {
            logger.info("Maintenance plans already exist for period {}-{}, found {} records",
                    startDate, endDate, existingPlans.size());
            return;
        }

        logger.info("No maintenance plans found for period {}-{}, attempting to import from file", startDate, endDate);

        // If file import failed, create default maintenance plans
        logger.info("Creating default maintenance plans");
        List<Maintenance> maintenancePlans = new ArrayList<>();

        // Create a map of vehicle IDs to Vehicle objects to avoid N+1 queries
        List<Vehicle> allVehicles = vehicleRepository.findAll();
        Map<String, Vehicle> vehicleMap = new HashMap<>();
        for (Vehicle vehicle : allVehicles) {
            vehicleMap.put(vehicle.getId(), vehicle);
        }

        // April 2025 plans
        addMaintenancePlan(maintenancePlans, vehicleMap, "TA01", LocalDate.of(2025, 4, 1));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TD01", LocalDate.of(2025, 4, 3));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TC01", LocalDate.of(2025, 4, 5));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TB01", LocalDate.of(2025, 4, 7));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TD02", LocalDate.of(2025, 4, 10));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TD03", LocalDate.of(2025, 4, 13));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TB02", LocalDate.of(2025, 4, 16));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TD04", LocalDate.of(2025, 4, 19));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TC02", LocalDate.of(2025, 4, 22));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TD05", LocalDate.of(2025, 4, 25));

        // May 2025 plans
        addMaintenancePlan(maintenancePlans, vehicleMap, "TA02", LocalDate.of(2025, 5, 1));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TD06", LocalDate.of(2025, 5, 3));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TC03", LocalDate.of(2025, 5, 5));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TB03", LocalDate.of(2025, 5, 7));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TD07", LocalDate.of(2025, 5, 10));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TD08", LocalDate.of(2025, 5, 13));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TB04", LocalDate.of(2025, 5, 16));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TD09", LocalDate.of(2025, 5, 19));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TC04", LocalDate.of(2025, 5, 22));
        addMaintenancePlan(maintenancePlans, vehicleMap, "TD10", LocalDate.of(2025, 5, 25));

        // Save all maintenance plans in a single operation
        if (!maintenancePlans.isEmpty()) {
            maintenanceRepository.saveAll(maintenancePlans);
            logger.info("Created {} default maintenance plans", maintenancePlans.size());
        } else {
            logger.warn("No default maintenance plans were created - check vehicle availability");
        }
    }

    /**
     * Helper method to add a maintenance plan to the list if the vehicle exists.
     */
    private void addMaintenancePlan(List<Maintenance> plans, Map<String, Vehicle> vehicleMap,
            String vehicleId, LocalDate date) {
        Vehicle vehicle = vehicleMap.get(vehicleId);
        if (vehicle != null) {
            plans.add(new Maintenance(vehicle, date));
        } else {
            logger.warn("Failed to create maintenance plan: Vehicle {} not found", vehicleId);
        }
    }

}