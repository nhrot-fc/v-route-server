package com.example.plgsystem.dto;

import com.example.plgsystem.model.Maintenance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para la entidad Maintenance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceDTO {
    private Long id;
    private String vehicleId;
    private LocalDate assignedDate;
    private LocalDateTime realStart;
    private LocalDateTime realEnd;
    private boolean active;
    private long durationHours;

    /**
     * Convierte una entidad Maintenance a MaintenanceDTO
     */
    public static MaintenanceDTO fromEntity(Maintenance maintenance) {
        if (maintenance == null) return null;
        
        return MaintenanceDTO.builder()
                .id(maintenance.getId())
                .vehicleId(maintenance.getVehicleId())
                .assignedDate(maintenance.getAssignedDate())
                .realStart(maintenance.getRealStart())
                .realEnd(maintenance.getRealEnd())
                .active(maintenance.getRealStart() != null && maintenance.getRealEnd() == null)
                .durationHours(maintenance.getDurationHours())
                .build();
    }
}
