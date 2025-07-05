package com.example.plgsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para la creación de un mantenimiento
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceCreateDTO {
    private String vehicleId;
    private LocalDate assignedDate;
}
