package com.example.plgsystem.assignation;

import java.time.LocalDateTime;
import com.example.plgsystem.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DeliveryPart {
    private final String orderId;
    private final int glpDeliverM3;
    private final LocalDateTime deadlineTime;
    
    @Override
    public String toString() {
        return String.format("📦 DeliveryPart { 🔖 orderId: %s, 🛢️ glpDeliverM3: %d, ⏰ deadline: %s }",
                orderId, glpDeliverM3, deadlineTime.format(Constants.DATE_TIME_FORMATTER));
    }
}
