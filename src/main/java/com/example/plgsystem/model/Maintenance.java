package com.example.plgsystem.model;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

    @Column(name = "repeat_months")
    private int repeatMonths = 2; // Default bimonthly repetition

    public Maintenance(String vehicleId, LocalDate date, MaintenanceType type) {
        this.vehicleId = vehicleId;
        this.startDate = date.atStartOfDay();
        this.endDate = date.atTime(LocalTime.MAX);
        this.type = type;
        this.completed = false;
        this.repeatMonths = 2;
    }

    public Maintenance(String vehicleId, LocalDateTime startDate, LocalDateTime endDate, MaintenanceType type, int repeatMonths) {
        this.vehicleId = vehicleId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.completed = false;
        this.repeatMonths = repeatMonths;
    }

    public LocalDate getDate() {
        return startDate.toLocalDate();
    }

    public long getDurationHours() {
        return ChronoUnit.HOURS.between(startDate, endDate);
    }

    public boolean isActiveAt(LocalDateTime dateTime) {
        return !dateTime.isBefore(startDate) && !dateTime.isAfter(endDate);
    }

    public Maintenance createNextTask() {
        LocalDateTime nextStart = startDate.plusMonths(repeatMonths);
        LocalDateTime nextEnd = endDate.plusMonths(repeatMonths);
        return new Maintenance(vehicleId, nextStart, nextEnd, type, repeatMonths);
    }

    public static Maintenance fromString(String record, MaintenanceType type) {
        try {
            String[] parts = record.split(":");
            if (parts.length != 2) return null;
            String datePart = parts[0];
            LocalDate date = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyyMMdd"));
            String vehicleId = parts[1];
            return new Maintenance(vehicleId, date, type);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return String.format("🔧 Maintenance: %s on %s from %s to %s",
                vehicleId,
                startDate.toLocalDate(),
                startDate.format(dateFormat),
                endDate.format(dateFormat));
    }

    public String toRecordString() {
        return String.format("%s:%s",
                startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                vehicleId);
    }
}
