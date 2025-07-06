package com.example.plgsystem.enums;

import com.example.plgsystem.model.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeración que define los tipos de vehículos y sus características
 */
@Getter
@RequiredArgsConstructor
public enum VehicleType {
    TA(Constants.TA_GLP_CAPACITY_M3, Constants.TA_GROSS_WEIGHT_TARA_TON),
    TB(Constants.TB_GLP_CAPACITY_M3, Constants.TB_GROSS_WEIGHT_TARA_TON),
    TC(Constants.TC_GLP_CAPACITY_M3, Constants.TC_GROSS_WEIGHT_TARA_TON),
    TD(Constants.TD_GLP_CAPACITY_M3, Constants.TD_GROSS_WEIGHT_TARA_TON);

    private final int capacityM3;
    private final double tareWeightTon;

    /**
     * Convierte el volumen de GLP en m³ a toneladas
     */
    public double convertGlpM3ToTon(int glpM3) {
        switch (this) {
            case TA:
                return (glpM3 * Constants.TA_GLP_WEIGHT_TON) / Constants.TA_GLP_CAPACITY_M3;
            case TB:
                return (glpM3 * Constants.TB_GLP_WEIGHT_TON) / Constants.TB_GLP_CAPACITY_M3;
            case TC:
                return (glpM3 * Constants.TC_GLP_WEIGHT_TON) / Constants.TC_GLP_CAPACITY_M3;
            case TD:
                return (glpM3 * Constants.TD_GLP_WEIGHT_TON) / Constants.TD_GLP_CAPACITY_M3;
            default:
                return 0.0;
        }
    }
}
