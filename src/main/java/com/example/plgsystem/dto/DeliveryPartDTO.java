package com.example.plgsystem.dto;

import java.time.LocalDateTime;

import com.example.plgsystem.assignation.DeliveryPart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transferring DeliveryPart information to the client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPartDTO {
    private String orderId;
    private int glpDeliverM3;
    private LocalDateTime deadlineTime;
    
    /**
     * Converts a DeliveryPart entity to a DeliveryPartDTO
     * 
     * @param deliveryPart The DeliveryPart to convert
     * @return A DeliveryPartDTO representation of the delivery part
     */
    public static DeliveryPartDTO fromEntity(DeliveryPart deliveryPart) {
        if (deliveryPart == null) {
            return null;
        }
        
        return DeliveryPartDTO.builder()
                .orderId(deliveryPart.getOrderId())
                .glpDeliverM3(deliveryPart.getGlpDeliverM3())
                .deadlineTime(deliveryPart.getDeadlineTime())
                .build();
    }
} 