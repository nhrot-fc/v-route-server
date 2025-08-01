package com.example.plgsystem.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@ToString(exclude = "serveRecords")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order implements Serializable, Comparable<Order> {
    @Id
    private String id;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @Column(name = "deadline_time", nullable = false)
    private LocalDateTime deadlineTime;

    @Column(name = "glp_request_m3", nullable = false)
    private int glpRequestM3;

    @Embedded
    private Position position;

    @Column(name = "remaining_glp_m3", nullable = false)
    private int remainingGlpM3;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ServeRecord> serveRecords = new ArrayList<>();

    @Builder
    public Order(String id, LocalDateTime arrivalTime, LocalDateTime deadlineTime, int glpRequestM3,
            Position position) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.deadlineTime = deadlineTime;
        this.glpRequestM3 = glpRequestM3;
        this.position = position;
        this.remainingGlpM3 = glpRequestM3;
    }

    @Transactional
    public ServeRecord recordDelivery(int deliveredVolumeM3, Vehicle vehicle, LocalDateTime serveDate) {
        ServeRecord record = new ServeRecord(vehicle, this, deliveredVolumeM3, serveDate);
        serveRecords.add(record);
        remainingGlpM3 = remainingGlpM3 - deliveredVolumeM3;
        return record;
    }

    @Transient
    public boolean isDelivered() {
        return remainingGlpM3 <= 0;
    }

    @Transient
    public boolean isOverdue(LocalDateTime referenceDateTime) {
        return referenceDateTime.isAfter(deadlineTime);
    }

    public Order copy() {
        Order copy = new Order(
                this.id,
                this.arrivalTime,
                this.deadlineTime,
                this.glpRequestM3,
                this.position != null ? this.position.clone() : null);
        copy.remainingGlpM3 = this.remainingGlpM3;

        // Deep copy of serveRecords without circular references
        copy.serveRecords = new ArrayList<>();
        // We only copy the essential data since ServeRecord has circular references
        // Full record details are maintained in the original objects
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Order order = (Order) o;
        return id.equals(order.id) && arrivalTime.equals(order.arrivalTime) && deadlineTime.equals(order.deadlineTime)
                && glpRequestM3 == order.glpRequestM3 && position.equals(order.position)
                && remainingGlpM3 == order.remainingGlpM3;
    }

    @Override
    public int compareTo(Order other) {
        // First criteria: deadline time
        // Second criteria: id
        int deadlineComparison = this.deadlineTime.compareTo(other.deadlineTime);
        if (deadlineComparison != 0) {
            return deadlineComparison;
        }
        return this.id.compareTo(other.id);
    }
}