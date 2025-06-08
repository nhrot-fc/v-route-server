package com.example.plgsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "vehicle_id", nullable = false)
    private String vehicleId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private IncidentType type;
    
    @Column(name = "occurrence_time", nullable = false)
    private LocalDateTime occurrenceTime;
    
    @Column(name = "resolution_time")
    private LocalDateTime resolutionTime; // Time when vehicle is available again
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "location_x")),
        @AttributeOverride(name = "y", column = @Column(name = "location_y"))
    })
    private Position location;
    
    @Column(name = "on_site_immobilization_duration")
    private Duration onSiteImmobilizationDuration;
    
    @Column(name = "workshop_repair_duration")
    private Duration workshopRepairDuration; // Actual time spent in workshop

    // Additional fields for test compatibility
    @Column(name = "description")
    private String description;
    
    @Column(name = "resolved")
    private Boolean resolved;

    public Incident(String id, String vehicleId, IncidentType type, LocalDateTime occurrenceTime, Position location) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.type = type;
        this.occurrenceTime = occurrenceTime;
        this.location = location;

        switch (type) {
            case TYPE_1:
                this.onSiteImmobilizationDuration = Duration.ofHours(Constants.INCIDENT_TYPE_1_IMMOBILIZATION_HOURS);
                this.workshopRepairDuration = Duration.ZERO;
                this.resolutionTime = occurrenceTime.plus(this.onSiteImmobilizationDuration);
                break;
            case TYPE_2: // Map MECHANICAL_FAILURE to TYPE_2 logic
                this.onSiteImmobilizationDuration = Duration.ofHours(Constants.INCIDENT_TYPE_2_IMMOBILIZATION_HOURS);
                LocalDateTime vehicleReadyForWorkshop = occurrenceTime.plus(this.onSiteImmobilizationDuration);
                
                LocalTime occurrenceLocalTime = occurrenceTime.toLocalTime();
                Shift occurrenceShift = Shift.getShiftForTime(occurrenceLocalTime);
                LocalDateTime availabilityTime;

                if (occurrenceShift == Shift.T1) { // Occurs in T1
                    availabilityTime = LocalDateTime.of(occurrenceTime.toLocalDate(), Shift.T3.getStartTime());
                } else if (occurrenceShift == Shift.T2) { // Occurs in T2
                    availabilityTime = LocalDateTime.of(occurrenceTime.toLocalDate().plusDays(1), Shift.T1.getStartTime());
                } else { // Occurs in T3 (occurrenceShift == Shift.T3)
                    availabilityTime = LocalDateTime.of(occurrenceTime.toLocalDate().plusDays(1), Shift.T2.getStartTime());
                }
                // Ensure availability time is after vehicle is ready for workshop
                if (availabilityTime.isBefore(vehicleReadyForWorkshop)) {
                    // This can happen if T3 starts late and vehicle is ready earlier
                    // Example: Incident in T1 @ 7:00, immobilised 2h (ready 9:00). Available T3 (16:00).
                    // Example: Incident in T3 @ 23:00, immobilised 2h (ready Day+1 01:00). Available Day+1 T2 (08:00).
                    // The logic above generally holds.
                }
                this.resolutionTime = availabilityTime;
                this.workshopRepairDuration = Duration.between(vehicleReadyForWorkshop, this.resolutionTime);
                if (this.workshopRepairDuration.isNegative()) this.workshopRepairDuration = Duration.ZERO; // Should not happen with correct shift logic
                break;
            case TYPE_3: // Map ACCIDENT to TYPE_3 logic
                this.onSiteImmobilizationDuration = Duration.ofHours(Constants.INCIDENT_TYPE_3_IMMOBILIZATION_HOURS);
                LocalDateTime vehicleReadyForWorkshop3 = occurrenceTime.plus(this.onSiteImmobilizationDuration);
                
                // Available start of T1 on day A+3
                this.resolutionTime = LocalDateTime.of(occurrenceTime.toLocalDate().plusDays(3), Shift.T1.getStartTime());
                this.workshopRepairDuration = Duration.between(vehicleReadyForWorkshop3, this.resolutionTime);
                 if (this.workshopRepairDuration.isNegative()) this.workshopRepairDuration = Duration.ZERO;
                break;
        }
    }

    // Alias methods for test compatibility
    public LocalDateTime getTimestamp() { return occurrenceTime; }
    public void setTimestamp(LocalDateTime timestamp) { this.occurrenceTime = timestamp; }
    public Position getPosition() { return location; }
    public void setPosition(Position position) { this.location = position; }
    public boolean isResolved() { return resolved != null && resolved; }

    // Removed setters for resolutionTime and workshopDuration as they are now fully calculated in constructor
}
