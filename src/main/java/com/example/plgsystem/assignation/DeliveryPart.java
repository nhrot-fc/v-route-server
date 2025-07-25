package com.example.plgsystem.assignation;

import java.time.LocalDateTime;
import com.example.plgsystem.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DeliveryPart implements Comparable<DeliveryPart> {
    private final String orderId;
    private final int glpDeliverM3;
    private final LocalDateTime deadlineTime;
    
    @Override
    public String toString() {
        return String.format("📦 DeliveryPart { 🔖 orderId: %s, 🛢️ glpDeliverM3: %d, ⏰ deadline: %s }",
                orderId, glpDeliverM3, deadlineTime.format(Constants.DATE_TIME_FORMATTER));
    }
    
    @Override
    public int compareTo(DeliveryPart other) {
        // First criteria: deadline time
        int deadlineComparison = this.deadlineTime.compareTo(other.deadlineTime);
        if (deadlineComparison != 0) {
            return deadlineComparison;
        }
        
        // Second criteria: order id
        int orderIdComparison = this.orderId.compareTo(other.orderId);
        if (orderIdComparison != 0) {
            return orderIdComparison;
        }
        
        // Third criteria: glp deliver amount
        return Integer.compare(this.glpDeliverM3, other.glpDeliverM3);
    }
}
