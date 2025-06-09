package com.example.plgsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order extends Stop {
    // immutable attributes
    @Id
    private String id;
    
    @Column(name = "arrive_date", nullable = false)
    private LocalDateTime arriveDate;
    
    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;
    
    @Column(name = "glp_request", nullable = false)
    @JsonProperty("glpRequest")
    private double glpRequest;

    // mutable attributes
    @Column(name = "delivery_date")
    @Setter
    @JsonProperty("deliveryDate")
    private LocalDateTime deliveryDate;
    
    @Column(name = "remaining_glp", nullable = false)
    @Setter
    @JsonProperty("remainingGLP")
    private double remainingGLP;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    public Order(String id, LocalDateTime arriveDate, LocalDateTime dueDate, double GLPRequest, Position position) {
        super(position);
        this.id = id;
        this.arriveDate = arriveDate;
        this.dueDate = dueDate;
        this.glpRequest = GLPRequest;
        this.remainingGLP = GLPRequest;
    }

    public Order(String id, LocalDateTime arriveDate, LocalDateTime dueDate, double volume, Position position,
            LocalDateTime deliveryDate) {
        super(position);
        this.id = id;
        this.arriveDate = arriveDate;
        this.dueDate = dueDate;
        this.glpRequest = volume;
        this.remainingGLP = volume;
        this.deliveryDate = deliveryDate;
    }

    public double getRemainingVolume() {
        return this.remainingGLP;
    }

    public void recordDelivery(double deliveredVolume, LocalDateTime actualDeliveryDate) {
        this.remainingGLP -= deliveredVolume;
        if (this.remainingGLP < Constants.EPSILON) { // Use EPSILON for floating point comparison
            this.remainingGLP = 0;
        }
        this.setDeliveryDate(actualDeliveryDate);
    }

    public long getRemainingMinutes(LocalDateTime currentDate) {
        if (this.dueDate.isBefore(currentDate))
            return 0;
        return java.time.Duration.between(currentDate, this.dueDate).toMinutes();
    }

    public boolean isOverdue(LocalDateTime currentDate) {
        return this.dueDate.isBefore(currentDate);
    }

    public double getUrgency(LocalDateTime currentDate) {
        double remainingMinutes = getRemainingMinutes(currentDate);
        if (remainingMinutes <= 0)
            return Double.MAX_VALUE; // Past due date = maximum urgency

        double remainingVol = getRemainingVolume();
        if (remainingVol <= 0)
            return 0; // Nothing left to deliver = no urgency

        // More volume and less time means higher urgency
        // Convert minutes to hours and add 1 to avoid division by very small numbers
        double remainingHours = (remainingMinutes / 60.0) + 1.0;

        // Higher remaining volume and less time = higher urgency score
        return remainingVol / remainingHours;
    }

    @Override
    public Order clone() {
        Order clonedOrder;
        if (this.deliveryDate != null) {
            clonedOrder = new Order(this.id, this.arriveDate, this.dueDate, this.glpRequest, this.getPosition().clone(),
                    this.deliveryDate);
        } else {
            clonedOrder = new Order(this.id, this.arriveDate, this.dueDate, this.glpRequest,
                    this.getPosition().clone());
        }
        clonedOrder.remainingGLP = this.remainingGLP; // Explicitly set remainingGLP for the clone
        return clonedOrder;
    }

    @Override
    public String toString() {
        // Only mark as "✅" (completed) if actually fully delivered
        String deliveryStatus = (remainingGLP < Constants.EPSILON) ? "✅" : "⏳";
        
        // Add a warning indicator for partially delivered orders
        if (remainingGLP > Constants.EPSILON && (glpRequest - remainingGLP > Constants.EPSILON)) {
            deliveryStatus = "⚠️"; // Partially delivered
        }
        
        // Format requested/delivered amounts
        return String.format("🚚 Order-%s %s [%.1f/%.1f m³] 📍%s", 
            id, 
            deliveryStatus,
            glpRequest - remainingGLP, 
            glpRequest,
            position.toString());
    }
}
