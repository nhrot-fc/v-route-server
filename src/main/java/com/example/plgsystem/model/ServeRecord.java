package com.example.plgsystem.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "serve_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServeRecord implements Serializable {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "glp_volume_m3", nullable = false)
    private int glpVolumeM3;

    @Column(name = "serve_date", nullable = false)
    private LocalDateTime serveDate;

    public ServeRecord(Vehicle vehicle, Order order, int glpVolumeM3, LocalDateTime serveDate) {
        this.id = UUID.randomUUID();
        this.vehicle = vehicle;
        this.order = order;
        this.glpVolumeM3 = glpVolumeM3;
        this.serveDate = serveDate;
    }
}