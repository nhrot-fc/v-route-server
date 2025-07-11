package com.example.plgsystem.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.plgsystem.assignation.Route;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transferring Route information to the client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDTO {
    private String vehicleId;
    private List<RouteStopDTO> stops;
    private LocalDateTime startTime;
    
    /**
     * Converts a Route entity to a RouteDTO
     * 
     * @param route The Route to convert
     * @return A RouteDTO representation of the route
     */
    public static RouteDTO fromEntity(Route route) {
        if (route == null) {
            return null;
        }
        
        return RouteDTO.builder()
                .vehicleId(route.vehicleId())
                .stops(route.stops().stream()
                        .map(RouteStopDTO::fromEntity)
                        .toList())
                .startTime(route.startTime())
                .build();
    }
} 