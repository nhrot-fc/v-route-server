package com.example.plgsystem.dto;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para crear un nuevo bloqueo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockageDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Position> blockageLines;
    
    /**
     * Convierte un BlockageCreateDTO a entidad Blockage
     */
    public Blockage toEntity() {
        return new Blockage(startTime, endTime, blockageLines);
    }
}
