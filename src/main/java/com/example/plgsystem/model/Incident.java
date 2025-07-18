package com.example.plgsystem.model;

import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "vehicle")
public class Incident implements Serializable {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Shift shift;

    @Column(name = "occurrence_time")
    private LocalDateTime occurrenceTime;

    @Embedded
    private Position location;

    @Column(nullable = false)
    private boolean resolved;

    public Incident(Vehicle vehicle, IncidentType type, LocalDateTime occurrenceTime) {
        this.id = UUID.randomUUID();
        this.vehicle = vehicle;
        this.type = type;
        this.occurrenceTime = occurrenceTime;
        this.shift = Shift.fromTime(occurrenceTime.toLocalTime());
        this.resolved = false;
    }

    @Transient
    public LocalDateTime getImmobilizationEndTime() {
        return occurrenceTime.plusHours(type.getImmobilizationHours());
    }

    @Transient
    public LocalDateTime getAvailabilityTime() {
        return getImmobilizationEndTime().plusHours(type.getRepairHours());
    }

    @Transient
    public boolean isReturnToDepotRequired() {
        return type.getRepairHours() > 0;
    }

    public void resolve() {
        this.resolved = true;
    }
    
    public Incident copy() {
        Incident copy = new Incident(this.vehicle, this.type, this.occurrenceTime);
        copy.setId(this.id);
        copy.setShift(this.shift);
        copy.setLocation(this.location);
        copy.setResolved(this.resolved);
        return copy;
    }
}