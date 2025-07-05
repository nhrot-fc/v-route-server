package com.example.plgsystem.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Vehicle {

    // Atributos inmutables
    private final String id;
    private final VehicleType type;
    private final double glpCapacity;          // m³
    private final double fuelCapacityGal;      // galones

    // Atributos mutables
    private Position currentPosition;
    private double currentGlp;                 // m³
    private double currentFuelGal;             // galones
    private VehicleStatus status;

    public Vehicle(String id, VehicleType type, Position currentPosition) {
        this.id = id;
        this.type = type;
        this.glpCapacity = type.getGlpCapacity();
        this.fuelCapacityGal = Constants.VEHICLE_FUEL_CAPACITY_GAL;
        this.currentPosition = currentPosition;
        this.currentGlp = 0;
        this.currentFuelGal = this.fuelCapacityGal;
        this.status = VehicleStatus.AVAILABLE;
    }

    public void setCurrentPosition(Position position) {
        this.currentPosition = position;
    }

    public void consumeFuel(double distanceKm) {
        double combinedWeight = type.convertGlpM3ToTon(currentGlp) + type.getTareWeightTon();
        double fuelConsumed = Math.abs((distanceKm * combinedWeight) / Constants.CONSUMPTION_FACTOR);
        currentFuelGal = Math.max(0, currentFuelGal - fuelConsumed);
    }

    public double calculateFuelNeeded(double distanceKm) {
        double combinedWeight = type.convertGlpM3ToTon(currentGlp) + type.getTareWeightTon();
        return Math.abs((distanceKm * combinedWeight) / Constants.CONSUMPTION_FACTOR);
    }

    public void refuel() {
        currentFuelGal = fuelCapacityGal;
    }

    public void dispenseGlp(double glpVolumeM3) {
        this.currentGlp = Math.max(0, this.currentGlp - Math.abs(glpVolumeM3));
    }

    public boolean canDispenseGLP(double glpVolumeM3) {
        return this.currentGlp >= Math.abs(glpVolumeM3);
    }

    public void refill(double glpVolumeM3) {
        this.currentGlp = Math.min(this.glpCapacity, this.currentGlp + Math.abs(glpVolumeM3));
    }

    public void serveOrder(Order order, double glpVolumeM3, LocalDateTime serveDate) {
        double volume = Math.abs(glpVolumeM3);
        this.dispenseGlp(volume);
        order.recordDelivery(volume, this.id, serveDate);
    }

    public Vehicle clone() {
        Vehicle clone = new Vehicle(this.id, this.type, this.currentPosition.clone());
        clone.currentGlp = this.currentGlp;
        clone.currentFuelGal = this.currentFuelGal;
        clone.status = this.status;
        return clone;
    }

    @Override
    public String toString() {
        return String.format("🚛 %s-%s %s [🛢️ %.1f/%.1f m³][⛽ %.1f/%.1f gal] %s",
                type.name(),
                id,
                status.name(),
                currentGlp,
                glpCapacity,
                currentFuelGal,
                fuelCapacityGal,
                currentPosition.toString());
    }
}
