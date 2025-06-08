package com.example.plgsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "maintenances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Maintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "vehicle_id", nullable = false)
    private String vehicleId;
    
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MaintenanceType type; // PREVENTIVE, CORRECTIVE
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "completed", nullable = false)
    private boolean completed;
    
    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    public Maintenance(String vehicleId, LocalDateTime startDate, MaintenanceType type) {
        this.vehicleId = vehicleId;
        this.startDate = startDate;
        this.type = type;
        this.completed = false;
        if (type == MaintenanceType.PREVENTIVE) {
            this.endDate = startDate.plusHours(Constants.PREVENTIVE_MAINTENANCE_UNAVAILABILITY_HOURS);
        }
        // For CORRECTIVE, endDate might be set differently based on incident details
    }
}
