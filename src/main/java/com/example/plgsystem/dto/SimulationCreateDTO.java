package com.example.plgsystem.dto;

import java.time.LocalDateTime;

import com.example.plgsystem.enums.SimulationType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimulationCreateDTO {
    private LocalDateTime startDateTime; // Simulated start date and time
    private LocalDateTime endDateTime;  // Used only for CUSTOM simulations
    private SimulationType type = SimulationType.CUSTOM;
    
    // Number of vehicles by type
    private int taVehicles = 0;
    private int tbVehicles = 0;
    private int tcVehicles = 0;
    private int tdVehicles = 0;
} 