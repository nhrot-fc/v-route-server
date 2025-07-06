package com.example.plgsystem.dto;

import com.example.plgsystem.model.Incident;
import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import com.example.plgsystem.model.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentDTO {
    private Long id;
    private String vehicleId;
    private IncidentType type;
    private Shift shift;
    private LocalDateTime occurrenceTime;
    private Position location;
    private boolean resolved;
    private double transferableGlp;
    private LocalDateTime availabilityTime;
    private boolean requiresReturnToDepot;
    
    // Conversión desde entidad a DTO
    public static IncidentDTO fromEntity(Incident incident) {
        return IncidentDTO.builder()
                .id(incident.getId())
                .vehicleId(incident.getVehicleId())
                .type(incident.getType())
                .shift(incident.getShift())
                .occurrenceTime(incident.getOccurrenceTime())
                .location(incident.getLocation())
                .resolved(incident.isResolved())
                .transferableGlp(incident.getTransferableGlp())
                .availabilityTime(incident.calculateAvailabilityTime())
                .requiresReturnToDepot(incident.requiresReturnToDepot())
                .build();
    }
    
    // Conversión desde DTO a entidad
    public Incident toEntity() {
        Incident incident = new Incident(vehicleId, type, shift);
        incident.setOccurrenceTime(occurrenceTime);
        incident.setLocation(location);
        incident.setResolved(resolved);
        incident.setTransferableGlp(transferableGlp);
        return incident;
    }
}
