package com.example.plgsystem.dto;

import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepotDTO {
    private String id;
    private Position position;
    private int glpCapacityM3;
    private DepotType type;
    private int currentGlpM3;

    public static DepotDTO fromEntity(Depot depot) {
        return DepotDTO.builder()
            .id(depot.getId())
            .position(depot.getPosition())
            .glpCapacityM3(depot.getGlpCapacityM3())
            .type(depot.getType())
            .currentGlpM3(depot.getCurrentGlpM3())
            .build();
    }

    public Depot toEntity() {
        return new Depot(id, position, glpCapacityM3, type);
    }
}
