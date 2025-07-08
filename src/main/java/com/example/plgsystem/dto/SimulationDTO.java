package com.example.plgsystem.dto;

import com.example.plgsystem.enums.SimulationStatus;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.simulation.Simulation;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimulationDTO {
    private UUID id;
    
    // Simulation world time
    private LocalDateTime simulatedCurrentTime;
    
    // Real world timestamps
    private LocalDateTime creationTime;
    private LocalDateTime realStartTime;
    private LocalDateTime realEndTime;
    
    private SimulationType type;
    private SimulationStatus status;

    public SimulationDTO(Simulation simulation) {
        this.id = simulation.getId();
        this.simulatedCurrentTime = simulation.getState().getCurrentTime();
        this.type = simulation.getType();
        this.status = simulation.getStatus();
        this.creationTime = simulation.getCreationTime();
        this.realStartTime = simulation.getRealStartTime();
        this.realEndTime = simulation.getRealEndTime();
    }
} 