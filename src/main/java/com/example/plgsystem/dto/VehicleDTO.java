package com.example.plgsystem.dto;

import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDTO {
    private String id;
    private VehicleType type;
    private int glpCapacityM3;
    private double fuelCapacityGal;
    private Position currentPosition;
    private int currentGlpM3;
    private double currentFuelGal;
    private VehicleStatus status;
    
    // Conversión desde entidad a DTO
    public static VehicleDTO fromEntity(Vehicle vehicle) {
        return VehicleDTO.builder()
                .id(vehicle.getId())
                .type(vehicle.getType())
                .glpCapacityM3(vehicle.getGlpCapacityM3())
                .fuelCapacityGal(vehicle.getFuelCapacityGal())
                .currentPosition(vehicle.getCurrentPosition())
                .currentGlpM3(vehicle.getCurrentGlpM3())
                .currentFuelGal(vehicle.getCurrentFuelGal())
                .status(vehicle.getStatus())
                .build();
    }
    
    // Conversión desde DTO a entidad
    public Vehicle toEntity() {
        Vehicle vehicle = Vehicle.builder()
                .id(id)
                .type(type)
                .currentPosition(currentPosition)
                .build();
                
        // La entidad maneja automáticamente estos valores al inicializar
        return vehicle;
    }
}
