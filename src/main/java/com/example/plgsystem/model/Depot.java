package com.example.plgsystem.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "depots")
@Getter
@NoArgsConstructor
public class Depot extends Stop {
    @Id
    private String id;
    
    @Column(name = "glp_capacity", nullable = false)
    @JsonProperty("glpCapacity")
    protected double glpCapacity;
    
    @Column(name = "fuel_capacity", nullable = false)
    @JsonProperty("fuelCapacity")
    protected double fuelCapacity;

    @Column(name = "current_glp", nullable = false)
    @Setter
    @JsonProperty("currentGLP")
    protected double currentGLP;
    
    @Column(name = "current_fuel", nullable = false)
    @Setter
    @JsonProperty("currentFuel")
    protected double currentFuel;

    @Column(name = "glp_min_threshold")
    @Setter
    @JsonProperty("glpMinThreshold")
    protected double glpMinThreshold = 0.0;

    public Depot(String id, Position position, double glpCapacity, double fuelCapacity) {
        super(position);
        this.id = id;
        this.glpCapacity = glpCapacity;
        this.fuelCapacity = fuelCapacity;
        this.currentGLP = 0;
        this.currentFuel = 0;
        this.glpMinThreshold = 0.0;
    }

    public Depot(String id, Position position, double glpCapacity, double fuelCapacity, double currentGLP, double currentFuel) {
        super(position);
        this.id = id;
        this.glpCapacity = glpCapacity;
        this.fuelCapacity = fuelCapacity;
        this.currentGLP = currentGLP;
        this.currentFuel = currentFuel;
        this.glpMinThreshold = 0.0;
    }
    
    public boolean canServe(double requestedGLP) { 
        return this.currentGLP - requestedGLP >= Constants.EPSILON; 
    }

    public void serve(double requestedGLP) {
        this.currentGLP = Math.max(this.currentGLP - requestedGLP, 0);
    }

    public void refill() { this.currentGLP = this.glpCapacity; }
    
    public void refillFuel() { this.currentFuel = this.fuelCapacity; }

    @Override
    public Depot clone() {
        Depot cloned = new Depot(
            this.id,
            this.position.clone(),
            this.glpCapacity,
            this.fuelCapacity,
            this.currentGLP,
            this.currentFuel
        );
        cloned.setGlpMinThreshold(this.glpMinThreshold);
        return cloned;
    }
    
    @Override
    public String toString() {
        return String.format("🏭 Depot-%s [GLP:%.1f/%.1f m³][⛽:%.1f/%.1f gal] %s", 
            id, 
            currentGLP, 
            glpCapacity,
            currentFuel,
            fuelCapacity,
            position.toString());
    }
}
