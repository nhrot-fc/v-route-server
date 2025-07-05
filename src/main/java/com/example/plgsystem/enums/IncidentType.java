package com.example.plgsystem.enums;

import com.example.plgsystem.model.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeración que define los tipos de incidentes que pueden ocurrir en los vehículos.
 * Cada tipo de incidente tiene diferentes características y consecuencias.
 */
@Getter
@RequiredArgsConstructor
public enum IncidentType {
    TYPE_1("Avería Tipo 1", 
          Constants.INCIDENT_TYPE_1_IMMOBILIZATION_HOURS, 
          0, 
          false, 
          "Incidente leve que puede ser resuelto en el lugar"),
          
    TYPE_2("Avería Tipo 2", 
          Constants.INCIDENT_TYPE_2_IMMOBILIZATION_HOURS, 
          4, 
          true, 
          "Incidente moderado que requiere reparación en el depósito"),
          
    TYPE_3("Avería Tipo 3", 
          Constants.INCIDENT_TYPE_3_IMMOBILIZATION_HOURS, 
          24, 
          true, 
          "Incidente grave que requiere reparación extensa");

    private final String name;
    private final int immobilizationHours;
    private final int repairHours;
    private final boolean returnToDepot;
    private final String description;
    
    /**
     * Verifica si el vehículo debe regresar al depósito después de este tipo de incidente
     */
    public boolean mustReturnToDepot() {
        return returnToDepot;
    }
}
