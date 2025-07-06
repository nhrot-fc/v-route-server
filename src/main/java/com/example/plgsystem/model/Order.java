package com.example.plgsystem.model;

import lombok.*;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un pedido de GLP en el sistema
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order implements Stop, Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    
    @Column(name = "arrive_time", nullable = false)
    private LocalDateTime arriveTime;
    
    @Column(name = "due_time", nullable = false)
    private LocalDateTime dueTime;
    
    @Column(name = "glp_request_m3", nullable = false)
    private int glpRequestM3;
    
    @Embedded
    private Position position;
    
    @Column(name = "remaining_glp_m3", nullable = false)
    private int remainingGlpM3;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServeRecord> serveRecords = new ArrayList<>();

    /**
     * Método setter para permitir la actualización del GLP restante
     */
    public void setRemainingGlpM3(int remainingGlpM3) {
        this.remainingGlpM3 = remainingGlpM3;
    }
    
    /**
     * Constructor principal para crear un nuevo pedido
     */
    @Builder
    public Order(String id, LocalDateTime arriveTime, LocalDateTime dueTime, int glpRequestM3, Position position) {
        this.id = id;
        this.arriveTime = arriveTime;
        this.dueTime = dueTime;
        this.glpRequestM3 = glpRequestM3;
        this.position = position;
        
        this.remainingGlpM3 = glpRequestM3;
    }
    
    /**
     * Registra una entrega parcial o total del pedido y retorna el registro de entrega
     */
    @Transactional
    public ServeRecord recordDelivery(int deliveredVolumeM3, String vehicleId, LocalDateTime serveDate) {
        remainingGlpM3 -= Math.abs(deliveredVolumeM3);
        remainingGlpM3 = Math.max(0, remainingGlpM3); // Asegurar que no sea negativo
        
        ServeRecord record = new ServeRecord(vehicleId, this.id, Math.abs(deliveredVolumeM3), serveDate);
        serveRecords.add(record);
        return record;
    }
    
    /**
     * Registra una entrega parcial o total del pedido con relaciones a entidades
     */
    @Transactional
    public ServeRecord recordDelivery(int deliveredVolumeM3, Vehicle vehicle, LocalDateTime serveDate) {
        remainingGlpM3 -= Math.abs(deliveredVolumeM3);
        remainingGlpM3 = Math.max(0, remainingGlpM3); // Asegurar que no sea negativo
        
        ServeRecord record = new ServeRecord(vehicle, this, Math.abs(deliveredVolumeM3), serveDate);
        serveRecords.add(record);
        return record;
    }
    
    /**
     * Verifica si el pedido ha sido entregado completamente
     */
    public boolean isDelivered() {
        return remainingGlpM3 <= 0;
    }
    
    /**
     * Verifica si el pedido está vencido en relación a una fecha de referencia
     */
    public boolean isOverdue(LocalDateTime referenceDateTime) {
        return referenceDateTime.isAfter(dueTime);
    }
    
    /**
     * Calcula el tiempo restante en minutos hasta el vencimiento del pedido
     */
    public int timeUntilDue(LocalDateTime referenceDateTime) {
        if (isDelivered())
            return 0;
        if (isOverdue(referenceDateTime))
            return -1;
            
        Duration duration = Duration.between(referenceDateTime, dueTime);
        long minutesUntilDue = duration.toMinutes();
        
        return (int) minutesUntilDue;
    }
    
    /**
     * Calcula la prioridad del pedido basado en su tiempo de vencimiento
     */
    public double calculatePriority(LocalDateTime referenceDateTime) {
        if (isDelivered())
            return 0.0;
        int minutesUntilDue = timeUntilDue(referenceDateTime);
        if (minutesUntilDue < 0)
            return 1000.0 + (-minutesUntilDue / 60.0);
        return 100.0 / (1.0 + (minutesUntilDue / 60.0));
    }
    
    @Override
    public String toString() {
        String status = isDelivered() ? "✅" : "⏳";
        return String.format("📦 %s %s [🕒 %s] [GLP: %d/%d m³] %s",
                id,
                status,
                dueTime.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")),
                remainingGlpM3,
                glpRequestM3,
                position);
    }
    
    /**
     * Crea una copia del pedido
     */
    public Order clone() {
        Order clonedOrder = Order.builder()
                .id(this.id)
                .arriveTime(this.arriveTime)
                .dueTime(this.dueTime)
                .glpRequestM3(this.glpRequestM3)
                .position(this.position.clone())
                .build();
                
        clonedOrder.remainingGlpM3 = this.remainingGlpM3;
        
        return clonedOrder;
    }
}
