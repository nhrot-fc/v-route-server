package com.example.plgsystem.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.plgsystem.operation.Action;
import com.example.plgsystem.operation.ActionType;
import com.example.plgsystem.model.Position;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transferring Action information to the client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionDTO {
    private ActionType type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Position> path;
    
    private int glpDelivered;
    private int glpLoaded;
    
    private double fuelConsumedGal;
    private double fuelRefueledGal;
    
    // Reference IDs
    private String orderId;
    private String depotId;
    
    // Progress
    private double progress;
    
    /**
     * Converts an Action entity to an ActionDTO
     * 
     * @param action The Action to convert
     * @return An ActionDTO representation of the action
     */
    public static ActionDTO fromEntity(Action action) {
        if (action == null) {
            return null;
        }
        
        return ActionDTO.builder()
                .type(action.getType())
                .startTime(action.getStartTime())
                .endTime(action.getEndTime())
                .path(action.getPath())
                .glpDelivered(action.getGlpDelivered())
                .glpLoaded(action.getGlpLoaded())
                .fuelConsumedGal(action.getFuelConsumedGal())
                .fuelRefueledGal(action.getFuelRefueledGal())
                .orderId(action.getOrderId())
                .depotId(action.getDepotId())
                .progress(action.getCurrentProgress())
                .build();
    }
} 