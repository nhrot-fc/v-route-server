package com.example.plgsystem.dto;

import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import com.example.plgsystem.model.Position;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentCreateDTO {
    private String vehicleId;
    private IncidentType type;
    private Shift shift;
    private LocalDateTime occurrenceTime;
    private Position location;
}
