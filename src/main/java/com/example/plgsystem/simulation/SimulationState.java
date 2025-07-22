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
        String vehicleId = incident.getVehicle().getId();
        Vehicle vehicle = getVehicleById(vehicleId);
        if (vehicle != null) {
            vehicle.setIncident();
            currentVehiclePlans.remove(vehicleId);
            incidents.add(incident);
        }
    }

    public void addMaintenance(Maintenance maintenance) {
        maintenances.add(maintenance);
    }

    public void addVehiclePlan(String vehicleId, VehiclePlan plan) {
        currentVehiclePlans.put(vehicleId, plan);
    }

    public VehiclePlan getVehiclePlan(String vehicleId) {
        return currentVehiclePlans.get(vehicleId);
    }

    public void removeVehiclePlan(String vehicleId) {
        currentVehiclePlans.remove(vehicleId);
    }

    public void refillDepots() {
        for (Depot depot : auxDepots) {
            if (depot != null) {
                depot.refill();
            }
        }
        if (mainDepot != null) {
            mainDepot.refill();
        }
    }

    public boolean isPositionBlockedAt(Position position, LocalDateTime time) {
        return blockages.stream().filter(b -> b.isActiveAt(time)).anyMatch(b -> b.isPositionBlocked(position));
    }

    public void advanceTime(Duration duration) {
        LocalDateTime nextTime = currentTime.plus(duration);
        processStateChanges(nextTime);
        PlanExecutor.executePlan(this, nextTime);
        currentTime = nextTime;
    }

    private void processStateChanges(LocalDateTime nextTime) {
        // Clean past orders, incidents, blockages, maintenances
        orders.removeIf(Order::isDelivered);
        blockages.removeIf(blockage -> nextTime.isAfter(blockage.getEndTime()));

        // Process incidents
        List<Incident> resolvedIncidents = new ArrayList<>();
        incidents.forEach(incident -> {
            if (nextTime.isAfter(incident.getAvailabilityTime())) {
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
                    nextTime.isAfter(maintenance.getRealEnd())) {
                maintenance.getVehicle().setAvailable();
                completedMaintenances.add(maintenance);
            }
        });
        maintenances.removeAll(completedMaintenances);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String topDivider = "══════════════════════════════════════════════════════════\n";
        String sectionDivider = "------------------------------------------------------\n";

        sb.append(topDivider);
        sb.append("📊                SIMULATION STATE                📊\n");
        sb.append(topDivider);
        sb.append("🕒 Current Time: ").append(currentTime.format(Constants.DATE_TIME_FORMATTER)).append("\n");

        // --- Infrastructure ---
        sb.append("🏢 INFRASTRUCTURE & FLEET 🚚\n");
        sb.append(sectionDivider);

        sb.append("🏭 Main Depot:\n");
        sb.append("  └─ ").append(mainDepot.toString()).append("\n");

        sb.append("🏬 Auxiliary Depots (").append(auxDepots.size()).append("):\n");
        auxDepots.forEach(depot -> sb.append("  └─ ").append(depot.toString()).append("\n"));

        sb.append("🚚 Vehicles (").append(vehicles.size()).append("):\n");
        vehicles.forEach(vehicle -> sb.append("  └─ ").append(vehicle.toString()).append("\n"));

        // --- Events and Dynamic Queues ---
        sb.append("📋 EVENTS & DYNAMIC QUEUES 🔄\n");
        sb.append(sectionDivider);

        sb.append("📦 Pending Orders (").append(orders.size()).append("):\n");
        orders.forEach(order -> sb.append("  └─ ").append(order.toString()).append("\n"));

        sb.append("🚧 Active Blockages (").append(blockages.size()).append("):\n");
        blockages.forEach(blockage -> sb.append("  └─ ").append(blockage.toString()).append("\n"));

        sb.append("⚠️ Active Incidents (").append(incidents.size()).append("):\n");
        incidents.forEach(incident -> sb.append("  └─ ").append(incident.toString()).append("\n"));

        sb.append("🔧 Ongoing Maintenance (").append(maintenances.size()).append("):\n");
        maintenances.forEach(maintenance -> sb.append("  └─ ").append(maintenance.toString()).append("\n"));

        sb.append(topDivider);
        return sb.toString();
    }

    public SimulationState createSnapshot() {
        // Crear copias profundas de todos los vehículos
        List<Vehicle> vehicleCopies = new ArrayList<>(vehicles.size());
        Map<String, Vehicle> vehicleMap = new HashMap<>(); // Para mantener relación entre ID y copia

        for (Vehicle vehicle : vehicles) {
            Vehicle vehicleCopy = vehicle.copy();
            vehicleCopies.add(vehicleCopy);
            vehicleMap.put(vehicle.getId(), vehicleCopy);
        }

        // Crear copias de los depósitos
        Depot mainDepotCopy = mainDepot.copy();
        List<Depot> auxDepotCopies = new ArrayList<>(auxDepots.size());
        for (Depot depot : auxDepots) {
            auxDepotCopies.add(depot.copy());
        }

        // Crear el nuevo estado de simulación
        SimulationState copy = new SimulationState(vehicleCopies, mainDepotCopy, auxDepotCopies, currentTime);

        // Copiar órdenes
        for (Order order : orders) {
            copy.addOrder(order.copy());
        }

        // Copiar bloqueos
        for (Blockage blockage : blockages) {
            copy.addBlockage(blockage.copy());
        }

        // Copiar incidentes (manteniendo relación con vehículos copiados)
        for (Incident incident : incidents) {
            Incident incidentCopy = incident.copy();
            // Reemplazar la referencia al vehículo con su copia
            Vehicle originalVehicle = incident.getVehicle();
            if (originalVehicle != null) {
                Vehicle copiedVehicle = vehicleMap.get(originalVehicle.getId());
                if (copiedVehicle != null) {
                    incidentCopy.setVehicle(copiedVehicle);
                }
            }
            copy.addIncident(incidentCopy);
        }

        // Copiar mantenimientos (manteniendo relación con vehículos copiados)
        for (Maintenance maintenance : maintenances) {
            Maintenance maintenanceCopy = maintenance.copy();
            // Reemplazar la referencia al vehículo con su copia
            Vehicle originalVehicle = maintenance.getVehicle();
            if (originalVehicle != null) {
                Vehicle copiedVehicle = vehicleMap.get(originalVehicle.getId());
                if (copiedVehicle != null) {
                    maintenanceCopy.setVehicle(copiedVehicle);
                }
            }
            copy.addMaintenance(maintenanceCopy);
        }

        // Copiar planes de vehículos
        for (Map.Entry<String, VehiclePlan> entry : currentVehiclePlans.entrySet()) {
            copy.addVehiclePlan(entry.getKey(), entry.getValue().copy());
        }

        return copy;
    }
}