package com.example.plgsystem.simulation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Vehicle;

public class SimulationState {
    private final List<Vehicle> vehicles;
    private final Depot mainDepot;
    private final List<Depot> auxDepots;

    private LocalDateTime currentTime;
    private final List<Order> orderQueue;
    private final List<Blockage> blockages;
    private final List<Incident> incidents;
    private final List<Maintenance> maintenances;

    public SimulationState(List<Vehicle> vehicles, Depot mainDepot, List<Depot> auxDepots,
            LocalDateTime referenceDateTime) {
        this.currentTime = referenceDateTime;
        this.vehicles = new ArrayList<>(vehicles);
        this.mainDepot = mainDepot;
        this.auxDepots = new ArrayList<>(auxDepots);
        this.orderQueue = new ArrayList<>();
        this.blockages = new ArrayList<>();
        this.incidents = new ArrayList<>();
        this.maintenances = new ArrayList<>();
    }

    public List<Vehicle> getVehicles() {
        return Collections.unmodifiableList(vehicles);
    }

    public List<Depot> getAuxDepots() {
        return Collections.unmodifiableList(auxDepots);
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public List<Order> getOrderQueue() {
        return Collections.unmodifiableList(orderQueue);
    }

    public List<Blockage> getBlockages() {
        return Collections.unmodifiableList(blockages);
    }

    public List<Incident> getIncidents() {
        return Collections.unmodifiableList(incidents);
    }

    public List<Maintenance> getMaintenances() {
        return Collections.unmodifiableList(maintenances);
    }

    public Depot getMainDepot() {
        return mainDepot;
    }

    public void setCurrentTime(LocalDateTime newTime) {
        this.currentTime = newTime;
    }

    public void addOrder(Order order) {
        orderQueue.add(order);
    }

    public void addOrders(List<Order> orders) {
        orderQueue.addAll(orders);
    }

    public List<Order> getPendingOrders() {
        return orderQueue.stream()
                .filter(order -> !order.isDelivered())
                .collect(Collectors.toList());
    }

    public List<Order> getOverdueOrders() {
        return orderQueue.stream()
                .filter(order -> !order.isDelivered() && order.isOverdue(currentTime))
                .collect(Collectors.toList());
    }

    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
    }

    public void addAuxDepot(Depot depot) {
        auxDepots.add(depot);
    }

    public void addBlockage(Blockage blockage) {
        blockages.add(blockage);
    }

    public void addBlockages(List<Blockage> blockages) {
        blockages.addAll(blockages);
    }

    public List<Blockage> getActiveBlockagesAt(LocalDateTime dateTime) {
        return blockages.stream()
                .filter(b -> b.isActiveAt(dateTime))
                .collect(Collectors.toList());
    }

    public void addIncident(Incident incident) {
        incidents.add(incident);
    }

    public void addIncidents(List<Incident> incidents) {
        incidents.addAll(incidents);
    }

    public List<Incident> getActiveIncidentsForVehicle(String vehicleId) {
        return incidents.stream()
                .filter(incident -> incident.getVehicleId().equals(vehicleId) &&
                        !incident.isResolved() &&
                        incident.getOccurrenceTime() != null &&
                        !currentTime.isBefore(incident.getOccurrenceTime()))
                .collect(Collectors.toList());
    }

    public void addMaintenanceTask(Maintenance task) {
        maintenances.add(task);
    }

    public void addMaintenanceTasks(List<Maintenance> tasks) {
        maintenances.addAll(tasks);
    }

    public boolean hasScheduledMaintenance(String vehicleId, LocalDateTime dateTime) {
        for (Maintenance task : maintenances) {
            if (task.getVehicleId().equals(vehicleId) && task.isActiveAt(dateTime)) {
                return true;
            }
        }
        return false;
    }

    public Maintenance getMaintenanceTaskForVehicle(String vehicleId, LocalDateTime dateTime) {
        for (Maintenance task : maintenances) {
            if (task.getVehicleId().equals(vehicleId) && task.isActiveAt(dateTime)) {
                return task;
            }
        }
        return null;
    }

    public void advanceTime(int minutes) {
        LocalDateTime previousTime = this.currentTime;
        this.currentTime = this.currentTime.plusMinutes(minutes);
        
        // Check if we've crossed into a new day (00:00)
        if (hasNewDayStarted(previousTime, this.currentTime)) {
            handleNewDayStarted();
        }
        
        updateEnvironmentState();
    }
    
    private boolean hasNewDayStarted(LocalDateTime previousTime, LocalDateTime currentTime) {
        // Check if we've crossed midnight (new day started)
        return !previousTime.toLocalDate().equals(currentTime.toLocalDate()) && 
               currentTime.getHour() == 0 && currentTime.getMinute() == 0;
    }
    
    private void handleNewDayStarted() {
        // Actions to perform when a new day starts at 00:00
        System.out.println("🌅 New day started at: " + currentTime.toLocalDate());
        
        // Refill all depots at the start of a new day
        mainDepot.refillGLP();
        for (Depot depot : auxDepots) {
            depot.refillGLP();
        }
        
        // Reset daily statistics or perform other daily tasks
        // You can add more daily reset logic here as needed
    }

    private void updateEnvironmentState() {
        for (Vehicle vehicle : vehicles) {
            if (hasScheduledMaintenance(vehicle.getId(), currentTime)) {
                vehicle.setStatus(VehicleStatus.MAINTENANCE);
                continue;
            }

            List<Incident> activeIncidents = getActiveIncidentsForVehicle(vehicle.getId());
            if (!activeIncidents.isEmpty()) {
                Incident mostRecent = activeIncidents.get(activeIncidents.size() - 1);
                if (!mostRecent.isResolved()) {
                    LocalDateTime availabilityTime = mostRecent.calculateAvailabilityTime();
                    if (currentTime.isBefore(availabilityTime)) {
                        vehicle.setStatus(VehicleStatus.INCIDENT);
                    } else {
                        vehicle.setStatus(VehicleStatus.AVAILABLE);
                    }
                }
            }

            if (vehicle.getStatus() != VehicleStatus.MAINTENANCE &&
                    vehicle.getStatus() != VehicleStatus.INCIDENT) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
            }
        }

        // remove delivered orders
        orderQueue.removeIf(Order::isDelivered);
        // remove past blockages
        blockages.removeIf(blockage -> blockage.getEndTime().isBefore(currentTime));
        // remove resolved incidents
        incidents.removeIf(incident -> incident.isResolved() || incident.getOccurrenceTime() == null);
        // remove past maintenance tasks
        maintenances.removeIf(task -> !task.isActiveAt(currentTime));
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicles.stream()
                .filter(vehicle -> vehicle.getStatus() != VehicleStatus.INCIDENT)
                .collect(Collectors.toList());
    }

    /**
     * Finds an order by its ID in the environment
     * 
     * @param orderId The ID of the order to find
     * @return The order if found, null otherwise
     */
    public Order findOrderById(String orderId) {
        return orderQueue.stream()
                .filter(order -> order.getId().equals(orderId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a depot by its ID in the environment
     * 
     * @param depotId The ID of the depot to find
     * @return The depot if found, null otherwise
     */
    public Depot findDepotById(String depotId) {
        // Check main depot first
        if (mainDepot.getId().equals(depotId)) {
            return mainDepot;
        }

        // Check auxiliary depots
        return auxDepots.stream()
                .filter(depot -> depot.getId().equals(depotId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a vehicle by its ID in the environment
     * 
     * @param vehicleId The ID of the vehicle to find
     * @return The vehicle if found, null otherwise
     */
    public Vehicle findVehicleById(String vehicleId) {
        return vehicles.stream()
                .filter(vehicle -> vehicle.getId().equals(vehicleId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("🌍 Environment: ")
                .append(currentTime.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));

        sb.append("\n📋 Vehicles (").append(vehicles.size()).append("):");
        for (Vehicle vehicle : vehicles) {
            sb.append("\n  ").append(vehicle);
        }

        sb.append("\n📋 Depots (").append(auxDepots.size()).append("):");
        for (Depot depot : auxDepots) {
            sb.append("\n  ").append(depot);
        }

        sb.append("\n📋 Orders (").append(orderQueue.size()).append("):");
        long pending = orderQueue.stream().filter(o -> !o.isDelivered() && !o.isOverdue(currentTime)).count();
        long delivered = orderQueue.stream().filter(Order::isDelivered).count();
        long overdue = orderQueue.stream().filter(o -> !o.isDelivered() && o.isOverdue(currentTime)).count();

        for (Order order : orderQueue) {
            if (order.isDelivered()) {
                // Optionally skip delivered orders in detailed list or handle as needed
            } else if (order.isOverdue(currentTime)) {
                sb.append("\n  ").append(order).append(" ⚠️ OVERDUE");
            } else {
                sb.append("\n  ").append(order);
            }
        }

        sb.append("\n  📊 Summary: ").append(pending).append(" pending, ")
                .append(overdue).append(" overdue, ")
                .append(delivered).append(" delivered");

        List<Blockage> currentBlockages = getActiveBlockagesAt(currentTime);
        sb.append("\n📋 Active Blockages (").append(currentBlockages.size()).append("):");
        for (Blockage blockage : currentBlockages) {
            sb.append("\n  ").append(blockage);
        }

        int activeIncidentCount = 0;
        sb.append("\n📋 Active Incidents:");
        for (Vehicle vehicle : vehicles) {
            List<Incident> activeIncidents = getActiveIncidentsForVehicle(vehicle.getId());
            for (Incident incident : activeIncidents) {
                if (!incident.isResolved() && incident.getOccurrenceTime() != null) {
                    sb.append("\n  ").append(incident);
                    activeIncidentCount++;
                }
            }
        }
        sb.append("\n  📊 Total: ").append(activeIncidentCount).append(" active incidents");

        int todayMaintenanceCount = 0;
        sb.append("\n📋 Today's Maintenance Tasks:");
        for (Maintenance task : maintenances) {
            if (task.isActiveAt(currentTime)) {
                sb.append("\n  ").append(task);
                todayMaintenanceCount++;
            }
        }
        sb.append("\n  📊 Total: ").append(todayMaintenanceCount).append(" maintenance tasks today");

        return sb.toString();
    }
}
