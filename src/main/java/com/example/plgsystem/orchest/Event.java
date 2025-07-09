package com.example.plgsystem.orchest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Represents an event in the system that may trigger replanning.
 * Events are ordered by their scheduled time.
 */
@AllArgsConstructor
@Getter
@ToString
public class Event implements Comparable<Event> {
    private final EventType type;
    private final LocalDateTime time;
    private final String entityId;  // ID of related entity (vehicle, order, blockage, etc.)
    private final Object data;      // Additional event data

    @Override
    public int compareTo(Event other) {
        return this.time.compareTo(other.time);
    }
}
