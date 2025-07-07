package com.example.plgsystem.dto;

import com.example.plgsystem.model.Maintenance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceDTO {
    private UUID id;
    private String vehicleId;
    private LocalDate assignedDate;
    private LocalDateTime realStart;
    private LocalDateTime realEnd;
    private boolean active;
    private long durationHours;

    public static MaintenanceDTO fromEntity(Maintenance maintenance) {
        if (maintenance == null)
            return null;

        boolean isActive = maintenance.getRealStart() != null && maintenance.getRealEnd() == null;
        long hours = 0;

        if (maintenance.getRealStart() != null && maintenance.getRealEnd() != null) {
            hours = ChronoUnit.HOURS.between(maintenance.getRealStart(), maintenance.getRealEnd());
        }

        return MaintenanceDTO.builder()
                .id(maintenance.getId())
                .vehicleId(maintenance.getVehicle().getId())
                .assignedDate(maintenance.getAssignedDate())
                .realStart(maintenance.getRealStart())
                .realEnd(maintenance.getRealEnd())
                .active(isActive)
                .durationHours(hours)
                .build();
    }
}
