package com.example.plgsystem.simulation;

import com.example.plgsystem.model.*;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.operation.Action;
import com.example.plgsystem.operation.ActionType;
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
        executeVehiclePlans(currentTime, nextTime);
        currentTime = nextTime;
    }

    private void processStateChanges(LocalDateTime nextTime) {
        // Clean past orders, incidents, blockages, maintenances
        orders.removeIf(Order::isDelivered);
        blockages.removeIf(blockage -> blockage.getEndTime().isBefore(nextTime));

        // Process incidents
        List<Incident> resolvedIncidents = new ArrayList<>();
        incidents.forEach(incident -> {
            if (incident.getAvailabilityTime().isBefore(nextTime)) {
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
                    maintenance.getRealEnd().isBefore(nextTime)) {
                maintenance.getVehicle().setAvailable();
                completedMaintenances.add(maintenance);
            }
        });
        maintenances.removeAll(completedMaintenances);
    }

    private void executeVehiclePlans(LocalDateTime prevTime, LocalDateTime nextTime) {
        if (currentVehiclePlans == null || currentVehiclePlans.isEmpty()) {
            return;
        }

        for (Map.Entry<String, VehiclePlan> entry : currentVehiclePlans.entrySet()) {
            String vehicleId = entry.getKey();
            VehiclePlan plan = entry.getValue();
            Vehicle vehicle = getVehicleById(vehicleId);

            if (vehicle == null) {
                continue; // Skip if vehicle not found
            }

            Action currentAction = plan.getCurrentAction();
            if (currentAction == null) {
                continue; // No more actions to execute
            }

            // If action hasn't started yet
            if (currentAction.getStartTime().isAfter(nextTime)) {
                continue; // Not time to execute this action yet
            }

            executeAction(currentAction, vehicle, prevTime, nextTime);

            // If action is complete after execution, advance to next action
            if (currentAction.getCurrentProgress() >= 1.0) {
                plan.advanceAction();
            }
        }
    }

    private void executeAction(Action action, Vehicle vehicle, LocalDateTime prevTime, LocalDateTime nextTime) {
        double previousProgress = action.getCurrentProgress();

        // Apply immediate effects if this is the first time we're processing this
        // action
        if (previousProgress == 0.0 && !action.isEffectApplied() &&
                action.getStartTime().equals(prevTime) || action.getStartTime().isBefore(prevTime)) {
            applyImmediateEffects(action, vehicle);
        }

        // Calculate progress based on time
        long totalDurationMillis = Duration.between(action.getStartTime(), action.getEndTime()).toMillis();
        if (totalDurationMillis <= 0) {
            action.setCurrentProgress(1.0);
            completeAction(action, vehicle);
            return;
        }

        LocalDateTime effectiveTime = nextTime.isBefore(action.getEndTime()) ? nextTime : action.getEndTime();
        long elapsedMillis = Duration.between(action.getStartTime(), effectiveTime).toMillis();
        double progress = Math.min(1.0, (double) elapsedMillis / totalDurationMillis);
        action.setCurrentProgress(progress);

        // Apply gradual effects
        if (action.getType() == ActionType.DRIVE) {
            applyGradualEffects(action, vehicle, progress, previousProgress);
        }

        // If action is complete
        if (progress >= 1.0) {
            completeAction(action, vehicle);
        }
    }

    private void applyImmediateEffects(Action action, Vehicle vehicle) {
        switch (action.getType()) {
            case REFUEL:
                vehicle.refuel();
                vehicle.setRefueling();
                break;

            case RELOAD:
                Depot loadingDepot = getDepotById(action.getDepotId());
                if (loadingDepot != null) {
                    int glpToLoad = action.getGlpLoaded();
                    loadingDepot.serve(glpToLoad);
                    vehicle.refill(glpToLoad);
                }
                vehicle.setReloading();
                break;

            case SERVE:
                Order order = getOrderById(action.getOrderId());
                if (order != null && !order.isDelivered()) {
                    int glpToDeliver = action.getGlpDelivered();
                    vehicle.serveOrder(order, glpToDeliver, action.getStartTime());
                }
                vehicle.setServing();
                break;

            case MAINTENANCE:
                vehicle.setMaintenance();
                break;

            case WAIT:
                vehicle.setIdle();
                break;

            case DRIVE:
                // For DRIVE, we set the initial position
                if (action.getPath() != null && !action.getPath().isEmpty()) {
                    vehicle.setCurrentPosition(action.getPath().get(0));
                }
                vehicle.setDriving();
                break;
        }
    }

    private void applyGradualEffects(Action action, Vehicle vehicle, double currentProgress, double previousProgress) {
        if (previousProgress >= currentProgress) {
            return; // No additional effects to apply
        }

        double effectMultiplier = currentProgress - previousProgress;

        if (action.getType() == ActionType.DRIVE) {
            // Update vehicle position
            updateVehiclePosition(vehicle, action, currentProgress);

            // Apply proportional fuel consumption
            double fuelToConsume = action.getFuelConsumedGal() * effectMultiplier;
            vehicle.consumeFuel(fuelToConsume);
        }
    }

    private void completeAction(Action action, Vehicle vehicle) {
        // Ensure final position is set correctly
        if (action.getPath() != null && !action.getPath().isEmpty()) {
            vehicle.setCurrentPosition(action.getPath().get(action.getPath().size() - 1));
        }

        // Apply any final effects
        if (action.getType() == ActionType.DRIVE && !action.isEffectApplied()) {
            vehicle.consumeFuel(action.getFuelConsumedGal());
            action.setEffectApplied(true);
        }

        // Mark action as complete
        action.setCurrentProgress(1.0);
        action.setEffectApplied(true);

        // Set vehicle to available after completing an action
        vehicle.setAvailable();
    }

    private void updateVehiclePosition(Vehicle vehicle, Action action, double progress) {
        List<Position> path = action.getPath();
        if (path == null || path.isEmpty()) {
            return;
        }

        if (path.size() == 1) {
            vehicle.setCurrentPosition(path.get(0));
            return;
        }

        if (progress <= 0) {
            vehicle.setCurrentPosition(path.get(0));
        } else if (progress >= 1.0) {
            vehicle.setCurrentPosition(path.get(path.size() - 1));
        } else {
            // Improved position interpolation for smoother movement
            double segmentLength = 1.0 / (path.size() - 1);
            int segmentIndex = (int) Math.min(path.size() - 2, Math.floor(progress / segmentLength));
            double segmentProgress = (progress - segmentIndex * segmentLength) / segmentLength;

            Position start = path.get(segmentIndex);
            Position end = path.get(segmentIndex + 1);

            double newX = start.getX() + segmentProgress * (end.getX() - start.getX());
            double newY = start.getY() + segmentProgress * (end.getY() - start.getY());

            vehicle.setCurrentPosition(new Position(newX, newY));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String topDivider = "══════════════════════════════════════════════════════════\n";
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

    /**
     * Creates a lightweight snapshot of the current simulation state for planning
     * purposes.
     * Esta implementación crea copias profundas de todos los objetos para asegurar
     * que no existan referencias compartidas entre el estado original y la copia.
     * 
     * @return A new SimulationState object representing the current state
     */
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