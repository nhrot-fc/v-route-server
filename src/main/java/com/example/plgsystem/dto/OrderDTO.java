package com.example.plgsystem.dto;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private String id;
    private LocalDateTime arriveTime;
    private LocalDateTime dueTime;
    private int glpRequestM3;
    private int remainingGlpM3;
    private Position position;
    private boolean delivered;
    
    // Conversión desde entidad a DTO
    public static OrderDTO fromEntity(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .arriveTime(order.getArriveTime())
                .dueTime(order.getDueTime())
                .glpRequestM3(order.getGlpRequestM3())
                .remainingGlpM3(order.getRemainingGlpM3())
                .position(order.getPosition())
                .delivered(order.isDelivered())
                .build();
    }
    
    // Conversión desde DTO a entidad
    public Order toEntity() {
        return Order.builder()
                .id(id != null && !id.isEmpty() ? id : java.util.UUID.randomUUID().toString())
                .arriveTime(arriveTime)
                .dueTime(dueTime)
                .glpRequestM3(glpRequestM3)
                .position(position)
                .build();
    }
}
