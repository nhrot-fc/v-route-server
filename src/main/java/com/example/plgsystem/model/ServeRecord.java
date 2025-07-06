package com.example.plgsystem.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Representa un registro de entrega de GLP
 */
@Entity
@Table(name = "serve_records")
@Getter
@NoArgsConstructor
public class ServeRecord implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "vehicle_id", nullable = false, insertable = false, updatable = false)
    private String vehicleId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
    
    @Column(name = "order_id", nullable = false, insertable = false, updatable = false)
    private String orderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(name = "volume_m3", nullable = false)
    private int volumeM3;
    
    @Column(name = "serve_date", nullable = false)
    private LocalDateTime serveDate;
    
    /**
     * Constructor para crear un nuevo registro de servicio
     */
    public ServeRecord(String vehicleId, String orderId, int volumeM3, LocalDateTime serveDate) {
        this.vehicleId = vehicleId;
        this.orderId = orderId;
        this.volumeM3 = volumeM3;
        this.serveDate = serveDate;
    }
    
    /**
     * Constructor con relaciones a entidades
     */
    public ServeRecord(Vehicle vehicle, Order order, int volumeM3, LocalDateTime serveDate) {
        this.vehicle = vehicle;
        this.vehicleId = vehicle.getId();
        this.order = order;
        this.orderId = order.getId();
        this.volumeM3 = volumeM3;
        this.serveDate = serveDate;
    }
    
    @Override
    public String toString() {
        return String.format("📝 Vehículo %s entregó %d m³ en %s",
                vehicleId, 
                volumeM3, 
                serveDate.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
    }
    
    /**
     * Crea una copia del registro de servicio
     */
    public ServeRecord clone() {
        return new ServeRecord(this.vehicleId, this.orderId, this.volumeM3, this.serveDate);
    }
}
