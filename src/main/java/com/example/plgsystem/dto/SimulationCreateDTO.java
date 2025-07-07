package com.example.plgsystem.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimulationCreateDTO {
    private LocalDateTime startDateTime;
    private List<String> vehicleIds;
    private String mainDepotId;
    private List<String> auxDepotIds;
} 