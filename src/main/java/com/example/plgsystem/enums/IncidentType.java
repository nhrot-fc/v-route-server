package com.example.plgsystem.enums;

import com.example.plgsystem.model.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IncidentType {
      TI1("Avería Tipo 1",
                  Constants.INCIDENT_TYPE_1_IMMOBILIZATION_HOURS,
                  0,
                  "Incidente leve que puede ser resuelto en el lugar"),

      TI2("Avería Tipo 2",
                  Constants.INCIDENT_TYPE_2_IMMOBILIZATION_HOURS,
                  4,
                  "Incidente moderado que requiere reparación en el depósito"),

      TI3("Avería Tipo 3",
                  Constants.INCIDENT_TYPE_3_IMMOBILIZATION_HOURS,
                  24,
                  "Incidente grave que requiere reparación extensa");

      private final String name;
      private final int immobilizationHours;
      private final int repairHours;
      private final String description;
}
