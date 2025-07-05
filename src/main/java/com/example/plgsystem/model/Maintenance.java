package com.example.plgsystem.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Representa un mantenimiento programado para un vehículo
 */
@Entity
@Table(name = "maintenances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Maintenance implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "vehicle_id", nullable = false)
    private String vehicleId;
    
    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;
    
    @Column(name = "real_start")
    @Setter
    private LocalDateTime realStart;
    
    @Column(name = "real_end")
    @Setter
    private LocalDateTime realEnd;
    
    /**
     * Constructor principal para crear un nuevo mantenimiento
     */
    public Maintenance(String vehicleId, LocalDate assignedDate) {
        this.vehicleId = vehicleId;
        this.assignedDate = assignedDate;
    }
    
    /**
     * Obtiene la fecha real del mantenimiento
     * @return La fecha real de inicio o null si no ha comenzado
     */
    @Transient
    public LocalDate getDate() {
        return realStart != null ? realStart.toLocalDate() : null;
    }
    
    /**
     * Calcula la duración del mantenimiento en horas
     * @return Duración en horas o 0 si no ha finalizado
     */
    @Transient
    public long getDurationHours() {
        return (realStart != null && realEnd != null) ? 
               ChronoUnit.HOURS.between(realStart, realEnd) : 0;
    }
    
    /**
     * Verifica si el mantenimiento está activo en una fecha y hora específica
     * @param dateTime La fecha y hora a verificar
     * @return true si el mantenimiento está activo, false en caso contrario
     */
    @Transient
    public boolean isActiveAt(LocalDateTime dateTime) {
        return realStart != null && realEnd != null && 
               !dateTime.isBefore(realStart) && !dateTime.isAfter(realEnd);
    }
    
    /**
     * Crea la siguiente tarea de mantenimiento programada (2 meses después)
     * @return Una nueva instancia de Maintenance
     */
    @Transient
    public Maintenance createNextTask() {
        if (realEnd == null) return null;
        LocalDate nextDate = realEnd.toLocalDate().plusMonths(2);
        return new Maintenance(vehicleId, nextDate);
    }
    
    /**
     * Crea una instancia de Maintenance a partir de una cadena de texto
     * @param record Cadena con formato "yyyyMMdd:vehicleId"
     * @return Una nueva instancia de Maintenance o null si el formato es inválido
     */
    public static Maintenance fromString(String record) {
        try {
            // Split the record into date and vehicle parts
            String[] parts = record.split(":");
            if (parts.length != 2) {
                return null;
            }
            
            // Parse the date part
            String datePart = parts[0];
            LocalDate date = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            // Parse the vehicle ID
            String vehicleId = parts[1];
            
            return new Maintenance(vehicleId, date);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String toString() {
        String status = (realStart != null && realEnd != null) ? "Completed" : 
                        (realStart != null) ? "In Progress" : "Scheduled";
        
        return String.format("🔧 Maintenance: %s on %s - Status: %s", 
                vehicleId, 
                assignedDate,
                status);
    }
    
    /**
     * Convierte el mantenimiento a una cadena de texto para almacenamiento
     * @return Cadena con formato "yyyyMMdd:vehicleId"
     */
    @Transient
    public String toRecordString() {
        LocalDate dateToUse = (realStart != null) ? realStart.toLocalDate() : assignedDate;
        return String.format("%s:%s", 
                dateToUse.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                vehicleId);
    }
}
