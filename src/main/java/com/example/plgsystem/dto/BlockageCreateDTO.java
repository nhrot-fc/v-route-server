package com.example.plgsystem.dto;

import com.example.plgsystem.model.Position;
import lombok.AllArgsConstructor;
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
public class BlockageCreateDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Position> blockageLines;
}
