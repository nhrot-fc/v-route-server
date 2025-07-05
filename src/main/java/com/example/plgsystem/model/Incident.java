package com.example.plgsystem.model;

import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Representa un incidente en un vehículo (avería/mal funcionamiento) que puede ocurrir durante las operaciones.
 * Según el README, los incidentes pueden ocurrir durante las rutas y tienen diferentes tipos con
 * diferentes efectos en la disponibilidad del vehículo.
 */
@Entity
@Table(name = "incidents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Incident implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "vehicle_id", nullable = false)
    private String vehicleId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Shift shift;
    
    @Column(name = "occurrence_time")
    @Setter
    private LocalDateTime occurrenceTime;
    
    @Embedded
    @Setter
    private Position location;
    
    @Column(nullable = false)
    @Setter
    private boolean resolved;
    
    @Column(name = "transferable_glp", nullable = false)
    @Setter
    private double transferableGlp;
    
    /**
     * Constructor principal para crear un nuevo incidente
     */
    public Incident(String vehicleId, IncidentType type, Shift shift) {
        this.vehicleId = vehicleId;
        this.type = type;
        this.shift = shift;
        this.resolved = false;
        this.transferableGlp = 0;
    }
    
    /**
     * Calcula cuando el vehículo estará disponible nuevamente después del incidente.
     * 
     * @return El LocalDateTime cuando el vehículo estará disponible de nuevo
     */
    @Transient
    public LocalDateTime calculateAvailabilityTime() {
        if (occurrenceTime == null) {
            return null;
        }
        
        // Primero agregamos el tiempo de inmovilización (el tiempo que el vehículo está detenido en la ubicación del incidente)
        LocalDateTime availabilityTime = occurrenceTime.plusHours(type.getImmobilizationHours());
        
        // Luego añadimos el tiempo de reparación si es necesario (tiempo en el depósito)
        if (type.getRepairHours() > 0) {
            availabilityTime = availabilityTime.plusHours(type.getRepairHours());
        }
        
        return availabilityTime;
    }
    
    /**
     * Verifica si el vehículo necesita regresar al depósito después de este incidente.
     * 
     * @return true si el vehículo debe regresar al depósito, false si puede continuar su ruta
     */
    @Transient
    public boolean requiresReturnToDepot() {
        return type.mustReturnToDepot();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Incident [").append(vehicleId).append(", ").append(type).append(", ").append(shift);
        
        if (occurrenceTime != null) {
            sb.append(", Occurred at: ").append(occurrenceTime);
            sb.append(", Location: ").append(location);
            sb.append(", Available at: ").append(calculateAvailabilityTime());
            sb.append(", Transferable GLP: ").append(transferableGlp).append(" m³");
            sb.append(", Status: ").append(resolved ? "Resolved" : "Active");
        } else {
            sb.append(", Not yet occurred");
        }
        
        sb.append("]");
        return sb.toString();
    }
}
