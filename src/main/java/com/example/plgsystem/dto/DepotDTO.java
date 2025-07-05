package com.example.plgsystem.dto;

import com.example.plgsystem.model.Position;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear o actualizar un depósito
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepotDTO {
    private String id;
    private Position position;
    private int glpCapacityM3;
    private boolean canRefuel;
    private int currentGlpM3;
}
