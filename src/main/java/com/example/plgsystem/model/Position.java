package com.example.plgsystem.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    private int x;
    private int y;

    public Position clone() {
        return new Position(this.x, this.y);
    }

    /**
     * Calculates the Euclidean distance between this position and another position
     * @param other The other position
     * @return Distance in coordinate units
     */
    public double distanceTo(Position other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return String.format("📍(%d,%d)", x, y);
    }
}
