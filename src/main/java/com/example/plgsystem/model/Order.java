package com.example.plgsystem.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.UuidGenerator; // Add this import

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // Import this

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order extends Stop {
    // immutable attributes
    @Id
    @GeneratedValue // Changed from @GeneratedValue(generator = "uuid2")
    @UuidGenerator  // Replaced @GenericGenerator
    @Column(columnDefinition = "VARCHAR(36)")
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

    // SETTERS para el CSV
    public void setId(String id) {
        this.id = id;
    }

    public void setArriveDate(LocalDateTime arriveDate) {
        this.arriveDate = arriveDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public void setGlpRequest(double glpRequest) {
        this.glpRequest = glpRequest;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

}
