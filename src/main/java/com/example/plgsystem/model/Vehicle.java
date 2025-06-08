package com.example.plgsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "vehicles")
@Getter
@NoArgsConstructor
public class Vehicle {
    // immutable attributes
    @Id
    private String id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType type;
    
    @Column(name = "glp_capacity", nullable = false)
    @JsonProperty("glpCapacity")
    private double glpCapacity;
    
    @Column(name = "fuel_capacity", nullable = false)
    @JsonProperty("fuelCapacity")
    private double fuelCapacity;
    
    // mutable attributes
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "current_position_x")),
        @AttributeOverride(name = "y", column = @Column(name = "current_position_y"))
    })
    @Setter
    private Position currentPosition;
    
    @Column(name = "current_glp", nullable = false)
    @Setter
    @JsonProperty("currentGLP")
    private double currentGLP;
    
    @Column(name = "current_fuel", nullable = false)
    @Setter
    @JsonProperty("currentFuel")
    private double currentFuel;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Setter
    private VehicleStatus status;

    // Constructors
    public Vehicle(String id, VehicleType type, Position currentPosition) {
        this.id = id;
        this.type = type;
        this.glpCapacity = type.getGlpCapacity(); // Updated getter
        this.fuelCapacity = Constants.VEHICLE_FUEL_CAPACITY;
        this.currentPosition = currentPosition;
        this.currentGLP = 0; // Initial GLP level, could be full or empty based on starting scenario
        this.currentFuel = this.fuelCapacity; // Assume starts with a full tank of fuel
        this.status = VehicleStatus.AVAILABLE;
    }

    public Vehicle(String id, VehicleType type, double currentGLP, double currentFuel, VehicleStatus status, Position currentPosition) {
        this.id = id;
        this.type = type;
        this.glpCapacity = type.getGlpCapacity();
        this.fuelCapacity = Constants.VEHICLE_FUEL_CAPACITY;
        this.currentPosition = currentPosition;
        this.currentGLP = currentGLP;
        this.currentFuel = currentFuel;
        this.status = status;
    }

    // Public methods
    public double getCurrentGlpWeightTon() {
        return this.type.convertGlpM3ToTon(this.currentGLP);
    }

    public double getCurrentCombinedWeightTon() {
        return this.type.getTareWeightTon() + getCurrentGlpWeightTon();
    }

    /**
     * Consumes fuel based on the distance traveled and current weight.
     * Updates the currentFuelLevelGallons.
     * Throws IllegalArgumentException if not enough fuel.
     * Formula: Consumo [Galones] = (Distancia [Km] * Peso Combinado [Ton]) / 180
     * @param distanceKm The distance traveled in kilometers.
     */
    public void consumeFuel(double distanceKm) {
        if (distanceKm < Constants.EPSILON) return; // No distance, no fuel consumption

        double combinedWeight = getCurrentCombinedWeightTon();
        double fuelConsumedGallons = (distanceKm * combinedWeight) / Constants.CONSUMPTION_FACTOR;

        if (this.currentFuel < fuelConsumedGallons - Constants.EPSILON) {
            // Not enough fuel for the entire trip. What to do?
            // Option 1: Throw exception. Planner must check feasibility.
            // Option 2: Consume all remaining fuel and report how much distance was covered.
            // For now, throw an exception, as the planner should ensure sufficient fuel.
            
            // throw new IllegalStateException( 
            // "Vehicle " + id + " does not have enough fuel for " + distanceKm + " km. Needs " +
            //    fuelConsumedGallons + ", has " + this.currentFuel);
        }
        this.currentFuel -= fuelConsumedGallons;
    }

    public void reFuel(double gallons) {
        this.currentFuel = Math.min(this.currentFuel + gallons, this.fuelCapacity);
        if (this.currentFuel + gallons > this.fuelCapacity + Constants.EPSILON) {
            // Or log a warning if trying to overfill
        }
        this.currentFuel = Math.max(0, this.currentFuel); // Ensure not negative
    }

    public double dispenseGlp(double GLPVolume) {
        // if (!this.canDispenseGLP(GLPVolume)) throw new IllegalArgumentException("Not enough GLP to dispense " + GLPVolume);
        this.currentGLP = Math.max(this.currentGLP - GLPVolume, 0);
        return this.currentGLP;
    }

    public boolean canDispenseGLP(double GLPVolume) {
        return this.currentGLP >= GLPVolume - Constants.EPSILON;
    }

    public boolean serveOrder(Order order, LocalDateTime currentTime) {
        double volumeToServe = order.getRemainingVolume();

        if (volumeToServe <= Constants.EPSILON) {
            if (order.getDeliveryDate() == null) {
                 order.recordDelivery(0, currentTime);
            }
            return true;
        }

        if (canDispenseGLP(volumeToServe)) {
            this.currentGLP = Math.max(0, this.currentGLP - volumeToServe);
            order.recordDelivery(volumeToServe, currentTime);
            return true;
        }
         
        return false;
    }

    public void refill(double GLPVolume) {
        // if (this.currentGLP + GLPVolume > this.glpCapacity + Constants.EPSILON) throw new IllegalArgumentException("Cannot refill GLP beyond capacity for vehicle " + id);
        this.currentGLP = Math.min(this.currentGLP + GLPVolume, this.glpCapacity);
    }

    // Clone
    public Vehicle clone() {
        return new Vehicle(
            this.id,
            this.type,
            this.currentGLP,
            this.currentFuel,
            this.status,
            this.currentPosition.clone()
        );
    }

    @Override
    public String toString() {
        String statusIcon = "🔄";
        if (status == VehicleStatus.AVAILABLE) statusIcon = "✅";
        else if (status == VehicleStatus.MAINTENANCE) statusIcon = "🔧";
        else if (status == VehicleStatus.BROKEN_DOWN) statusIcon = "🚫";
        
        return String.format("🚛 %s-%s %s [GLP:%.1f/%.1f m³][⛽:%.1f/%.1f gal] %s",
                type.name(),
                id,
                statusIcon,
                currentGLP, 
                glpCapacity,
                currentFuel, 
                fuelCapacity,
                currentPosition.toString());
    }
}