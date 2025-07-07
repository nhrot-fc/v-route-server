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
    private LocalDateTime currentTime;
    private SimulationType type;
    private SimulationStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public SimulationDTO(Simulation simulation) {
        this.id = simulation.getId();
        this.currentTime = simulation.getState().getCurrentTime();
        this.type = simulation.getType();
        this.status = simulation.getStatus();
        this.startTime = simulation.getStartTime();
        this.endTime = simulation.getEndTime();
    }
} 