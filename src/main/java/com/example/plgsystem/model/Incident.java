package com.example.plgsystem.model;
import java.time.LocalDateTime;

/**
 * Represents a vehicle incident (breakdown/malfunction) that can occur during operations.
 * According to the README, incidents can happen during routes and have different types with
 * different effects on vehicle availability.
 */
public class Incident {
    private final String vehicleId;
    private final IncidentType type;
    private final Shift shift;
    private LocalDateTime occurrenceTime;
    private Position location;
    private boolean resolved = false;
    private double transferableGlp = 0;

    /**
     * Creates a new incident for a vehicle in a specific shift.
     *
     * @param vehicleId The ID of the vehicle affected by the incident
     * @param type The type of incident
     * @param shift The shift when the incident occurs
     */
    public Incident(String vehicleId, IncidentType type, Shift shift) {
        this.vehicleId = vehicleId;
        this.type = type;
        this.shift = shift;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public IncidentType getType() {
        return type;
    }

    public Shift getShift() {
        return shift;
    }

    public LocalDateTime getOccurrenceTime() {
        return occurrenceTime;
    }

    public void setOccurrenceTime(LocalDateTime occurrenceTime) {
        this.occurrenceTime = occurrenceTime;
    }

    public Position getLocation() {
        return location;
    }

    public void setLocation(Position location) {
        this.location = location;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved() {
        this.resolved = true;
    }

    public double getTransferableGlp() {
        return transferableGlp;
    }

    public void setTransferableGlp(double transferableGlp) {
        this.transferableGlp = transferableGlp;
    }

    public LocalDateTime calculateAvailabilityTime() {
        if (occurrenceTime == null) {
            return null;
        }
        LocalDateTime availabilityTime = occurrenceTime.plusHours(type.getImmobilizationHours());
        if (type.getRepairHours() > 0) {
            availabilityTime = availabilityTime.plusHours(type.getRepairHours());
        }
        return availabilityTime;
    }

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
