package com.example.plgsystem.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor

public class Environment {
    private final LocalDateTime currentTime;
    private final List<Vehicle> vehicles;
    private final List<Depot> depots;
    private final List<Maintenance> maintenances;
    private final List<Incident> incidents;
    private final List<Blockage> blockages;

    public List<Vehicle> getAvailableVehicles() {
        return vehicles.stream().filter(v -> v.getStatus() == VehicleStatus.AVAILABLE).collect(Collectors.toList());
    }
    
    @Override
    public String toString() {
        return "Environment{" +
               "currentTime=" + currentTime +
               ", vehicles=" + vehicles +
               ", storages=" + depots +
               ", maintenances=" + maintenances +
               ", incidents=" + incidents +
               ", blockedStreets=" + blockages +
               '}';
    }
}
