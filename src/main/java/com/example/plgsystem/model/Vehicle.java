package com.example.plgsystem.model;

import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.operation.Action;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = { "incidents", "serveRecords", "maintenances" })
public class Vehicle implements Serializable {
    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType type;

    @Column(name = "glp_capacity_m3", nullable = false)
    private int glpCapacityM3;

    @Column(name = "fuel_capacity_gal", nullable = false)
    private double fuelCapacityGal;

    @Embedded
    private Position currentPosition;

    @Column(name = "current_glp_m3", nullable = false)
    private int currentGlpM3;

    @Column(name = "current_fuel_gal", nullable = false)
    private double currentFuelGal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Incident> incidents = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServeRecord> serveRecords = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Maintenance> maintenances = new ArrayList<>();
    
    @Transient
    private Action currentAction; // Tracks the current action being performed

    @Builder
    public Vehicle(String id, VehicleType type, Position currentPosition) {
        this.id = id;
        this.type = type;
        this.glpCapacityM3 = type.getCapacityM3();
        this.fuelCapacityGal = Constants.VEHICLE_FUEL_CAPACITY_GAL;
        this.currentPosition = currentPosition;
        this.currentGlpM3 = 0;
        this.currentFuelGal = this.fuelCapacityGal;
        this.status = VehicleStatus.AVAILABLE;
        this.currentAction = null;
    }

    @Transactional
    public ServeRecord serveOrder(Order order, int glpVolumeM3, LocalDateTime serveDate) {
        this.currentGlpM3 = Math.max(0, this.currentGlpM3 - glpVolumeM3);
        return order.recordDelivery(glpVolumeM3, this, serveDate);
    }

    @Transient
    public double calculateFuelNeeded(double distanceKm) {
        double combinedWeight = (this.currentGlpM3 * Constants.GLP_DENSITY_M3_TON)
                + this.type.getTareWeightTon();
        return (distanceKm * combinedWeight) / Constants.CONSUMPTION_FACTOR;
    }

    @Transient
    public void consumeFuel(double fuelConsumedGal) {
        this.currentFuelGal = Math.max(0, this.currentFuelGal - fuelConsumedGal);
    }

    @Transient
    public void refuel() {
        this.currentFuelGal = this.fuelCapacityGal;
    }

    @Transient
    public void refill(int glpVolumeM3) {
        this.currentGlpM3 = Math.min(this.glpCapacityM3, this.currentGlpM3 + glpVolumeM3);
    }

    @Transient
    public void dispense(int glpVolumeM3) {
        this.currentGlpM3 = Math.max(0, this.currentGlpM3 - glpVolumeM3);
    }

    @Transient
    public boolean canDispense(int glpVolumeM3) {
        return this.currentGlpM3 >= glpVolumeM3;
    }

    @Transient
    public void setAvailable() {
        this.status = VehicleStatus.AVAILABLE;
    }

    @Transient
    public void setDriving() {
        this.status = VehicleStatus.DRIVING;
    }

    @Transient
    public void setRefueling() {
        this.status = VehicleStatus.REFUELING;
    }

    @Transient
    public void setReloading() {
        this.status = VehicleStatus.RELOADING;
    }

    @Transient
    public void setServing() {
        this.status = VehicleStatus.SERVING;
    }

    @Transient
    public void setIncident() {
        this.status = VehicleStatus.INCIDENT;
    }

    @Transient
    public void setIdle() {
        this.status = VehicleStatus.IDLE;
    }

    @Transient
    public void setMaintenance() {
        this.status = VehicleStatus.MAINTENANCE;
    }

    @Transient
    public boolean isAvailable() {
        return this.status != VehicleStatus.MAINTENANCE && this.status != VehicleStatus.INCIDENT;
    }

    @Transient
    public boolean isPerformingAction() {
        return this.currentAction != null;
    }

    @Transient
    public void setCurrentAction(Action action) {
        this.currentAction = action;
    }

    @Transient
    public void clearCurrentAction() {
        this.currentAction = null;
    }

    @Transient
    public LocalDateTime getCurrentActionEndTime() {
        return currentAction != null ? currentAction.getEndTime() : null;
    }

    public Vehicle copy() {
        // Create a deep copy with cloned position
        Vehicle copy = new Vehicle(this.id, this.type,
                this.currentPosition != null ? this.currentPosition.clone() : null);
        copy.currentGlpM3 = this.currentGlpM3;
        copy.currentFuelGal = this.currentFuelGal;
        copy.status = this.status;
        // Copy the current action if it exists
        copy.currentAction = this.currentAction != null ? this.currentAction.copy() : null;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Vehicle vehicle = (Vehicle) o;
        return id.equals(vehicle.id) && type == vehicle.type && glpCapacityM3 == vehicle.glpCapacityM3
                && fuelCapacityGal == vehicle.fuelCapacityGal && currentPosition.equals(vehicle.currentPosition)
                && currentGlpM3 == vehicle.currentGlpM3 && currentFuelGal == vehicle.currentFuelGal
                && status == vehicle.status;
    }
}
