package com.example.plgsystem.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order extends Stop {
    // === Immutable Attributes ===
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "arrive_date", nullable = false)
    private LocalDateTime arriveDate;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "glp_request", nullable = false)
    @JsonProperty("glpRequest")
    private double glpRequest;

    // === Mutable Attributes ===
    @Column(name = "remaining_glp", nullable = false)
    @Setter
    @JsonProperty("remainingGLP")
    private double remainingGLP;

    @Column(name = "delivery_date")
    @Setter
    @JsonProperty("deliveryDate")
    private LocalDateTime deliveryDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Transient
    private List<ServeRecord> records = new ArrayList<>();

    // === Constructors ===
    public Order(String id, LocalDateTime arriveDate, LocalDateTime dueDate, double glpRequest, Position position) {
        super(position);
        this.id = id;
        this.arriveDate = arriveDate;
        this.dueDate = dueDate;
        this.glpRequest = glpRequest;
        this.remainingGLP = glpRequest;
        this.records = new ArrayList<>();
    }

    public Order(String id, LocalDateTime arriveDate, LocalDateTime dueDate, double glpRequest,
                 Position position, LocalDateTime deliveryDate) {
        super(position);
        this.id = id;
        this.arriveDate = arriveDate;
        this.dueDate = dueDate;
        this.glpRequest = glpRequest;
        this.remainingGLP = glpRequest;
        this.deliveryDate = deliveryDate;
        this.records = new ArrayList<>();
    }

    // === Logic ===
    public boolean isDelivered() {
        return remainingGLP < Constants.EPSILON;
    }

    public boolean isOverdue(LocalDateTime referenceDateTime) {
        return referenceDateTime.isAfter(dueDate);
    }

    public long getRemainingMinutes(LocalDateTime currentDate) {
        if (this.dueDate.isBefore(currentDate)) return 0;
        return Duration.between(currentDate, dueDate).toMinutes();
    }

    public double getUrgency(LocalDateTime referenceDateTime) {
        if (isDelivered()) return 0.0;
        long remainingMinutes = getRemainingMinutes(referenceDateTime);
        if (remainingMinutes <= 0) return Double.MAX_VALUE;
        return remainingGLP / ((remainingMinutes / 60.0) + 1.0);
    }

    public double calculatePriority(LocalDateTime referenceDateTime) {
        if (isDelivered()) return 0.0;
        int minutesUntilDue = timeUntilDue(referenceDateTime);
        if (minutesUntilDue < 0) return 1000.0 + (-minutesUntilDue / 60.0);
        return 100.0 / (1.0 + (minutesUntilDue / 60.0));
    }

    public int timeUntilDue(LocalDateTime referenceDateTime) {
        if (isDelivered()) return 0;
        if (isOverdue(referenceDateTime)) return -1;
        return (int) Duration.between(referenceDateTime, dueDate).toMinutes();
    }

    public void recordDelivery(double deliveredVolume, String vehicleId, LocalDateTime serveDate) {
        this.remainingGLP -= Math.abs(deliveredVolume);
        this.remainingGLP = Math.max(0, this.remainingGLP);
        this.records.add(new ServeRecord(vehicleId, this.id, Math.abs(deliveredVolume), serveDate));
        this.deliveryDate = serveDate;
    }

    // === Cloning ===
    @Override
    public Order clone() {
        Order clone = new Order(this.id, this.arriveDate, this.dueDate, this.glpRequest, this.position.clone(), this.deliveryDate);
        clone.remainingGLP = this.remainingGLP;
        clone.records = new ArrayList<>(this.records);
        return clone;
    }

    // === Display ===
    @Override
    public String toString() {
        String statusIcon = isDelivered() ? "✅" : "⏳";
        if (remainingGLP > Constants.EPSILON && glpRequest - remainingGLP > Constants.EPSILON) {
            statusIcon = "⚠️";
        }

        return String.format("📦 %s %s [🕒 %s] [GLP: %.1f/%.1f m³] %s",
                id,
                statusIcon,
                dueDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")),
                glpRequest - remainingGLP,
                glpRequest,
                position.toString());
    }

    // === CSV setters (opcionales) ===
    public void setId(String id) { this.id = id; }
    public void setArriveDate(LocalDateTime arriveDate) { this.arriveDate = arriveDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public void setGlpRequest(double glpRequest) { this.glpRequest = glpRequest; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
