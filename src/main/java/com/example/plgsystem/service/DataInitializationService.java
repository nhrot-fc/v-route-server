package com.example.plgsystem.service;

import com.example.plgsystem.model.*;
import com.example.plgsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test") // Exclude from test profile
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private VehicleRepository vehicleRepository;
    
    @Autowired
    private DepotRepository depotRepository;

    @Override
    public void run(String... args) throws Exception {
        // Initialize default data if database is empty
        if (vehicleRepository.count() == 0) {
            initializeVehicles();
        }
        
        if (depotRepository.count() == 0) {
            initializeDepots();
        }
    }

    private void initializeVehicles() {
        // Create vehicles according to fleet specification from Constants
        
        // TA vehicles (2 units)
        for (int i = 1; i <= Constants.TA_UNITS; i++) {
            String id = String.format("TA%02d", i);
            Vehicle vehicle = new Vehicle(id, VehicleType.TA, Constants.CENTRAL_STORAGE_LOCATION);
            vehicleRepository.save(vehicle);
        }
        
        // TB vehicles (4 units)
        for (int i = 1; i <= Constants.TB_UNITS; i++) {
            String id = String.format("TB%02d", i);
            Vehicle vehicle = new Vehicle(id, VehicleType.TB, Constants.CENTRAL_STORAGE_LOCATION);
            vehicleRepository.save(vehicle);
        }
        
        // TC vehicles (4 units)
        for (int i = 1; i <= Constants.TC_UNITS; i++) {
            String id = String.format("TC%02d", i);
            Vehicle vehicle = new Vehicle(id, VehicleType.TC, Constants.CENTRAL_STORAGE_LOCATION);
            vehicleRepository.save(vehicle);
        }
        
        // TD vehicles (10 units)
        for (int i = 1; i <= Constants.TD_UNITS; i++) {
            String id = String.format("TD%02d", i);
            Vehicle vehicle = new Vehicle(id, VehicleType.TD, Constants.CENTRAL_STORAGE_LOCATION);
            vehicleRepository.save(vehicle);
        }
    }

    private void initializeDepots() {
        // Central Storage (main plant)
        Depot centralDepot = new Depot("CENTRAL", 
                                     Constants.CENTRAL_STORAGE_LOCATION, 
                                     1000.0, // Large capacity - assumed unlimited
                                     1000.0);
        centralDepot.refill(); // Start with full GLP
        centralDepot.refillFuel(); // Start with full fuel
        depotRepository.save(centralDepot);
        
        // North Intermediate Storage
        Depot northDepot = new Depot("NORTH", 
                                   Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 
                                   160.0, // From requirements
                                   100.0);
        northDepot.refill(); // Start with full capacity
        depotRepository.save(northDepot);
        
        // East Intermediate Storage  
        Depot eastDepot = new Depot("EAST", 
                                  Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 
                                  160.0, // From requirements
                                  100.0);
        eastDepot.refill(); // Start with full capacity
        depotRepository.save(eastDepot);
    }
}
