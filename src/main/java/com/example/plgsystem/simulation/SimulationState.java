package com.example.plgsystem.simulation;

import com.example.plgsystem.model.*;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.operation.VehiclePlan;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Getter
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

    private final Map<String, VehiclePlan> currentVehiclePlans;

    public SimulationState(List<Vehicle> vehicles, Depot mainDepot, List<Depot> auxDepots,
            LocalDateTime referenceDateTime) {
        this.currentTime = referenceDateTime;
        this.mainDepot = mainDepot;
        this.vehicles = new ArrayList<>(vehicles);
        this.auxDepots = new ArrayList<>(auxDepots);
        this.currentVehiclePlans = new HashMap<>();
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
        orders.removeIf(Order::isDelivered);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String topDivider =    "══════════════════════════════════════════════════════════\n";
        String sectionDivider = "------------------------------------------------------\n";

        sb.append(topDivider);
        sb.append("📊                SIMULATION STATE                📊\n");
        sb.append(topDivider);
        sb.append("🕒 Current Time: ").append(currentTime.format(Constants.DATE_TIME_FORMATTER)).append("\n\n");

        // --- Infrastructure ---
        sb.append("🏢 INFRASTRUCTURE & FLEET 🚚\n");
        sb.append(sectionDivider);
        
        sb.append("🏭 Main Depot:\n");
        sb.append("  └─ ").append(mainDepot.toString()).append("\n\n");

        sb.append("🏬 Auxiliary Depots (").append(auxDepots.size()).append("):\n");
        if (auxDepots.isEmpty()) {
            sb.append("  └─ None\n");
        } else {
            auxDepots.forEach(depot -> sb.append("  └─ ").append(depot.toString()).append("\n"));
        }
        sb.append("\n");

        sb.append("🚚 Vehicles (").append(vehicles.size()).append("):\n");
        if (vehicles.isEmpty()) {
            sb.append("  └─ None\n");
        } else {
            vehicles.forEach(vehicle -> sb.append("  └─ ").append(vehicle.toString()).append("\n"));
        }
        sb.append("\n");

        // --- Events and Dynamic Queues ---
        sb.append("📋 EVENTS & DYNAMIC QUEUES 🔄\n");
        sb.append(sectionDivider);
        
        sb.append("📦 Pending Orders (").append(orders.size()).append("):\n");
        if (orders.isEmpty()) {
            sb.append("  └─ None\n");
        } else {
            orders.forEach(order -> sb.append("  └─ ").append(order.toString()).append("\n"));
        }
        sb.append("\n");

        sb.append("🚧 Active Blockages (").append(blockages.size()).append("):\n");
        if (blockages.isEmpty()) {
            sb.append("  └─ None\n");
        } else {
            blockages.forEach(blockage -> sb.append("  └─ ").append(blockage.toString()).append("\n"));
        }
        sb.append("\n");

        sb.append("⚠️ Active Incidents (").append(incidents.size()).append("):\n");
        if (incidents.isEmpty()) {
            sb.append("  └─ None\n");
        } else {
            incidents.forEach(incident -> sb.append("  └─ ").append(incident.toString()).append("\n"));
        }
        sb.append("\n");

        sb.append("🔧 Ongoing Maintenance (").append(maintenances.size()).append("):\n");
        if (maintenances.isEmpty()) {
            sb.append("  └─ None\n");
        } else {
            maintenances.forEach(maintenance -> sb.append("  └─ ").append(maintenance.toString()).append("\n"));
        }
        
        sb.append(topDivider);
        return sb.toString();
    }
}