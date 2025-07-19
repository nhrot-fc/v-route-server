package com.example.plgsystem.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "maintenances")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Maintenance implements Serializable {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(name = "real_start")
    private LocalDateTime realStart;

    @Column(name = "real_end")
    private LocalDateTime realEnd;

    public Maintenance(Vehicle vehicle, LocalDate assignedDate) {
        this.id = UUID.randomUUID();
        this.vehicle = vehicle;
        this.assignedDate = assignedDate;
    }

    @Transient
    public Maintenance createNextTask() {
        if (this.realEnd == null)
            return null;
        LocalDate nextDate = this.realEnd.toLocalDate().plusMonths(2);
        return new Maintenance(this.vehicle, nextDate);
    }

    public Maintenance copy() {
        // En una copia para simulación, mantenemos la referencia al vehículo
        // pero sin crear copias recursivas que generarían relaciones circulares
        Maintenance copy = new Maintenance();
        copy.id = this.id;
        copy.assignedDate = this.assignedDate;
        copy.realStart = this.realStart;
        copy.realEnd = this.realEnd;
        copy.vehicle = this.vehicle; // Mantenemos referencia al vehículo original
        return copy;
    }
}