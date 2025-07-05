package com.example.plgsystem.model;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
public enum VehicleType {
    TA(25.0, 2.5, 12.5, 25.0),
    TB(15.0, 2.0, 7.5, 15.0),
    TC(10.0, 1.5, 5.0, 10.0),
    TD(5.0, 1.0, 2.5, 5.0);

    private final double capacityM3;
    private final double tareWeightTon;
    private final double maxGlpWeightTon;
    private final double glpCapacity;

    VehicleType(double capacityM3, double tareWeightTon, double maxGlpWeightTon, double glpCapacity) {
        this.capacityM3 = capacityM3;
        this.tareWeightTon = tareWeightTon;
        this.maxGlpWeightTon = maxGlpWeightTon;
        this.glpCapacity = glpCapacity;
    }

    public double getCombinedWeightWhenFullTon() {
        return tareWeightTon + maxGlpWeightTon;
    }

    public static final double GLP_DENSITY_TON_PER_M3 = 0.5;

    public double convertGlpM3ToTon(double glpM3) {
        return glpM3 * GLP_DENSITY_TON_PER_M3;
    }

    @Override
    public String toString() {
        return String.format("%s [🚚 %.1f t | 🛢️ %.0f m³]",
                name(),
                tareWeightTon,
                capacityM3);
    }

    public double getGlpCapacity() {
    return this.glpCapacity;
}

}
