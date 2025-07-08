package com.example.plgsystem.orchest;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents an event in the system that may trigger replanning.
 * Events are ordered by their scheduled time.
 */
@Getter
public class Event implements Comparable<Event> {
    private final EventType type;
    private final LocalDateTime time;
    @Setter
    private String entityId;  // ID of related entity (vehicle, order, blockage, etc.)
    @Setter
    private Object data;      // Additional event data

    public Event(EventType type, LocalDateTime time, String entityId, Object data) {
        this.type = type;
        this.time = time;
        this.entityId = entityId;
        this.data = data;
    }

    public Event(EventType type, LocalDateTime time) {
        this(type, time, null, null);
    }

    @Override
    public int compareTo(Event other) {
        return this.time.compareTo(other.time);
    }

    @Override
    public String toString() {
        return String.format("Event[%s, time=%s, entity=%s]", 
            type, time.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), 
            entityId != null ? entityId : "N/A");
    }
}
