package com.example.plgsystem.dto;

import java.time.LocalDateTime;

import com.example.plgsystem.assignation.RouteStop;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transferring RouteStop information to the client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStopDTO {
    private boolean isOrderStop;
    private String orderId;
    private LocalDateTime orderDeadlineTime;
    private String depotId;
    private int glpDeliverM3;
    private int glpLoadM3;
    
    /**
     * Converts a RouteStop entity to a RouteStopDTO
     * 
     * @param routeStop The RouteStop to convert
     * @return A RouteStopDTO representation of the route stop
     */
    public static RouteStopDTO fromEntity(RouteStop routeStop) {
        if (routeStop == null) {
            return null;
        }
        
        return RouteStopDTO.builder()
                .isOrderStop(routeStop.isOrderStop())
                .orderId(routeStop.getOrderId())
                .orderDeadlineTime(routeStop.getOrderDeadlineTime())
                .depotId(routeStop.getDepotId())
                .glpDeliverM3(routeStop.getGlpDeliverM3())
                .glpLoadM3(routeStop.getGlpLoadM3())
                .build();
    }
} 