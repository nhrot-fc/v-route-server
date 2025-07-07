package com.example.plgsystem.dto;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private String id;
    private LocalDateTime arrivalTime;
    private LocalDateTime deadlineTime;
    private int glpRequestM3;
    private int remainingGlpM3;
    private Position position;
    private boolean delivered;

    public static OrderDTO fromEntity(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .arrivalTime(order.getArrivalTime())
                .deadlineTime(order.getDeadlineTime())
                .glpRequestM3(order.getGlpRequestM3())
                .remainingGlpM3(order.getRemainingGlpM3())
                .position(order.getPosition())
                .delivered(order.isDelivered())
                .build();
    }

    public Order toEntity() {
        return new Order(
                id != null && !id.isEmpty() ? id : UUID.randomUUID().toString(),
                arrivalTime,
                deadlineTime,
                glpRequestM3,
                position);
    }
}
