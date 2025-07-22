package com.example.plgsystem.enums;

import com.example.plgsystem.model.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
/**
 * Enumeration for the different types of vehicle incidents.
 * Each incident type has different immobilization time, repair requirements, and return-to-depot behavior.
 */
@Getter
@RequiredArgsConstructor
public enum IncidentType {
      TI1("Avería Tipo 1",
                  Constants.INCIDENT_TYPE_1_IMMOBILIZATION_HOURS,
                  0,
                  false,
                  "Incidente leve que puede ser resuelto en el lugar"),

      TI2("Avería Tipo 2",
                  Constants.INCIDENT_TYPE_2_IMMOBILIZATION_HOURS,
                  0, // Special handling for this type - one shift in repair
                  true,
                  "Incidente moderado que requiere reparación en el depósito"),

      TI3("Avería Tipo 3",
                  Constants.INCIDENT_TYPE_3_IMMOBILIZATION_HOURS,
                  0, // Special handling for this type - 3 days in repair
                  true,
                  "Incidente grave que requiere reparación extensa");

      private final String name;
      private final int immobilizationHours;
      private final int repairHours;
      private final boolean returnToDepotRequired;
      private final String description;
      
      /**
       * Calculates the time when the vehicle will be available again after an incident.
       * 
       * @param occurrenceTime The time when the incident occurred
       * @param shift The shift when the incident occurred
       * @return The time when the vehicle will be available again
       */
      public LocalDateTime calculateAvailabilityTime(LocalDateTime occurrenceTime, Shift shift) {
          // First add immobilization time (vehicle is immobilized on site)
          LocalDateTime endImmobilizationTime = occurrenceTime.plusHours(immobilizationHours);
          
          // Special handling for different incident types
          switch (this) {
              case TI1:
                  // Type 1: Just return after immobilization (no additional repair time)
                  return endImmobilizationTime;
                  
              case TI2:
                  // Type 2: Available after one shift in repair
                  return calculateTI2AvailabilityTime(occurrenceTime, shift);
                  
              case TI3:
                  // Type 3: Available after 3 full days in repair
                  return endImmobilizationTime.plusDays(3).withHour(0).withMinute(0).withSecond(0);
                  
              default:
                  return endImmobilizationTime.plusHours(repairHours);
          }
      }
      
      /**
       * Calculates availability time specifically for TI2 incidents.
       * TI2 incidents make the vehicle unavailable for one shift.
       */
      private LocalDateTime calculateTI2AvailabilityTime(LocalDateTime occurrenceTime, Shift shift) {
          LocalDateTime endImmobilizationTime = occurrenceTime.plusHours(immobilizationHours);
          
          switch (shift) {
              case T1:
                  // Occurs in T1 (00:00-07:59), available in T3 same day
                  return endImmobilizationTime.withHour(16).withMinute(0).withSecond(0);
                  
              case T2:
                  // Occurs in T2 (08:00-15:59), available in T1 next day
                  return endImmobilizationTime.plusDays(1).withHour(0).withMinute(0).withSecond(0);
                  
              case T3:
                  // Occurs in T3 (16:00-23:59), available in T2 next day
                  return endImmobilizationTime.plusDays(1).withHour(8).withMinute(0).withSecond(0);
                  
              default:
                  return endImmobilizationTime.plusHours(8); // Fallback - one shift is approximately 8 hours
          }
      }
      
      /**
       * Determines if the vehicle needs to return to the main depot after immobilization.
       * 
       * @return true if the vehicle needs to return to the main depot, false otherwise
       */
      public boolean requiresReturnToDepot() {
          return returnToDepotRequired;
      }
}
