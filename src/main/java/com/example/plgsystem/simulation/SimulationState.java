package com.example.plgsystem.simulation;

import com.example.plgsystem.model.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Component
@Scope("prototype")
public class SimulationState {
    @Setter
    private LocalDateTime currentTime;

    // --- Colecciones Inmutables (referencia) pero contenido mutable ---
    private final List<Vehicle> vehicles;
    private final Depot mainDepot;
    private final List<Depot> auxDepots;

    // --- Colas y eventos dinámicos de la simulación ---
    private final List<Order> orders = new ArrayList<>();
    private final List<Blockage> blockages = new ArrayList<>();
    private final List<Incident> incidents = new ArrayList<>();
    private final List<Maintenance> maintenances = new ArrayList<>();

    public SimulationState(List<Vehicle> vehicles, Depot mainDepot, List<Depot> auxDepots,
            LocalDateTime referenceDateTime) {
        this.currentTime = referenceDateTime;
        this.mainDepot = mainDepot;
        this.vehicles = new ArrayList<>(vehicles);
        this.auxDepots = new ArrayList<>(auxDepots);
    }

    public Vehicle getVehicleById(String id) {
        return vehicles.stream()
                .filter(vehicle -> vehicle.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public Depot getDepotById(String id) {
        if (id.equals(mainDepot.getId())) {
            return mainDepot;
        }
        return auxDepots.stream()
                .filter(depot -> depot.getId().equals(id))
                .findFirst().orElse(null);
    }

    public Order getOrderById(String id) {
        return orders.stream()
                .filter(order -> order.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    public void addBlockage(Blockage blockage) {
        blockages.add(blockage);
    }

    public void addIncident(Incident incident) {
        incidents.add(incident);
    }

    public void addMaintenance(Maintenance maintenance) {
        maintenances.add(maintenance);
    }

    public void advanceTime(Duration duration) {
        currentTime = currentTime.plus(duration);
        processStateChanges();
    }
    
    public boolean isPositionBlockedAt(Position position, LocalDateTime time) {
        return blockages.stream().filter(b -> b.isActiveAt(time)).anyMatch(b -> b.isPositionBlocked(position));
    }

    /**
     * Updates internal state based on current time
     */
    private void processStateChanges() {
        // Clean past orders, incidents, blockages, maintenances
        orders.removeIf(order -> order.isDelivered());
        blockages.removeIf(blockage -> blockage.getEndTime().isBefore(currentTime));

        // Process incidents
        List<Incident> resolvedIncidents = new ArrayList<>();
        incidents.forEach(incident -> {
            if (incident.getAvailabilityTime().isBefore(currentTime)) {
                incident.setResolved(true);
                incident.getVehicle().setAvailable();
                resolvedIncidents.add(incident);
            }
        });
        incidents.removeAll(resolvedIncidents);

        // Process maintenances
        List<Maintenance> completedMaintenances = new ArrayList<>();
        maintenances.forEach(maintenance -> {
            if (maintenance.getRealEnd() != null && 
                maintenance.getRealEnd().isBefore(currentTime)) {
                maintenance.getVehicle().setAvailable();
                completedMaintenances.add(maintenance);
            }
        });
        maintenances.removeAll(completedMaintenances);
    }
}