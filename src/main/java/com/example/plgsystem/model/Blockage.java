package com.example.plgsystem.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

@Entity
@Table(name = "blockages")
@Getter
@Setter
@NoArgsConstructor
public class Blockage implements Serializable {
    @Id
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    // Format: "x1,y1,x2,y2,x3,y3,..."
    @Column(nullable = false, length = 4000)
    private String linePoints;

    @Transient
    private List<Position> lines;

    public Blockage(LocalDateTime startTime, LocalDateTime endTime, List<Position> blockageLines) {
        this.id = UUID.randomUUID();
        this.startTime = startTime;
        this.endTime = endTime;
        this.lines = new ArrayList<>(blockageLines);
        this.linePoints = serializePositions(blockageLines);
    }

    public static String serializePositions(List<Position> positions) {
        return positions.stream()
                .map(pos -> (int) pos.getX() + "," + (int) pos.getY())
                .collect(Collectors.joining(","));
    }

    public static List<Position> deserializePositions(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return Collections.emptyList();
        }

        String[] points = serialized.split(",");
        List<Position> result = new ArrayList<>(points.length / 2);

        for (int i = 0; i < points.length; i += 2) {
            int x = Integer.parseInt(points[i]);
            int y = Integer.parseInt(points[i + 1]);
            result.add(new Position(x, y));
        }

        return result;
    }

    @PostLoad
    private void postLoad() {
        this.lines = deserializePositions(this.linePoints);
    }

    public List<Position> getLines() {
        if (lines == null && linePoints != null) {
            lines = deserializePositions(linePoints);
        }
        return lines;
    }

    public boolean isActiveAt(LocalDateTime dateTime) {
        return !dateTime.isBefore(startTime) && !dateTime.isAfter(endTime);
    }

    public boolean isPositionBlocked(Position position) {
        List<Position> lineas = getLines();
        for (int i = 0; i < lineas.size() - 1; i++) {
            Position p1 = lineas.get(i);
            Position p2 = lineas.get(i + 1);

            // Vertical segment (same X)
            if (p1.getX() == p2.getX() && p1.getX() == position.getX()) {
                double minY = Math.min(p1.getY(), p2.getY());
                double maxY = Math.max(p1.getY(), p2.getY());
                if (minY <= position.getY() && position.getY() <= maxY) {
                    return true;
                }
            }
            // Horizontal segment (same Y)
            else if (p1.getY() == p2.getY() && p1.getY() == position.getY()) {
                double minX = Math.min(p1.getX(), p2.getX());
                double maxX = Math.max(p1.getX(), p2.getX());
                if (minX <= position.getX() && position.getX() <= maxX) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🚧 Blockage { 🕒 startTime: %s, 🕒 endTime: %s\n", startTime, endTime));

        for (Position line : lines) {
            sb.append("  └> ").append(line.toString()).append("\n");
        }
        sb.append("}\n");

        return sb.toString();
    }

    public Blockage copy() {
        Blockage copy = new Blockage();
        copy.setId(this.id);
        copy.setStartTime(this.startTime);
        copy.setEndTime(this.endTime);
        copy.setLinePoints(this.linePoints);
        copy.setLines(new ArrayList<>(this.lines));
        return copy;
    }
}
