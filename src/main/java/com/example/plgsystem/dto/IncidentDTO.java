package com.example.plgsystem.dto;

import com.example.plgsystem.model.Incident;
import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentDTO {
    private UUID id;
    private String vehicleId;
    private IncidentType type;
    private Shift shift;
    private LocalDateTime occurrenceTime;
    private boolean resolved;
    private LocalDateTime immobilizationEndTime;
    private LocalDateTime availabilityTime;
    private boolean returnToDepotRequired;

    public static IncidentDTO fromEntity(Incident incident) {
        return IncidentDTO.builder()
                .id(incident.getId())
                .vehicleId(incident.getVehicle().getId())
                .type(incident.getType())
                .shift(incident.getShift())
                .occurrenceTime(incident.getOccurrenceTime())
                .resolved(incident.isResolved())
                .immobilizationEndTime(incident.getImmobilizationEndTime())
                .availabilityTime(incident.getAvailabilityTime())
                .returnToDepotRequired(incident.isReturnToDepotRequired())
                .build();
    }
}
