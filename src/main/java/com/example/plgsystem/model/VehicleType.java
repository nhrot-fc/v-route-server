package com.example.plgsystem.model;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor

public enum VehicleType {
    TA(25.0, 2.5, 12.5), // capacityM3, tareWeightTon, maxGlpWeightTon
    TB(15.0, 2.0, 7.5),
    TC(10.0, 1.5, 5.0),
    TD(5.0, 1.0, 2.5);

    private final double glpCapacity;
    private final double tareWeightTon;
    private final double maxGlpWeightTon; // Weight of GLP when tank is full

    public double getCombinedWeightWhenFullTon() {
        return this.tareWeightTon + this.maxGlpWeightTon;
    }

    // GLP density: e.g., TA: 12.5 Ton / 25 m³ = 0.5 Ton/m³
    // This density is consistent across all vehicle types based on README data.
    public static final double GLP_DENSITY_TON_PER_M3 = 0.5;

    public double convertGlpM3ToTon(double glpM3) {
        return glpM3 * GLP_DENSITY_TON_PER_M3;
    }
}
