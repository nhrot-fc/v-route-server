package com.example.plgsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "blockages")
public class Blockage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "start_node_x")),
        @AttributeOverride(name = "y", column = @Column(name = "start_node_y"))
    })
    private Position startNode;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "end_node_x")),
        @AttributeOverride(name = "y", column = @Column(name = "end_node_y"))
    })
    private Position endNode;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // No persistentes: usados para lógica de simulación
    @Transient
    private List<Position> lines;

    @Transient
    private Set<Position> blockagePoints;

    // Constructor público para lógica (VRP-solver)
    public Blockage(Position startNode, Position endNode, LocalDateTime startTime, LocalDateTime endTime) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.startTime = startTime;
        this.endTime = endTime;

        this.lines = Arrays.asList(startNode, endNode);
        this.blockagePoints = precomputePoints(this.lines);
    }

    // Constructor requerido por JPA
    public Blockage() {}

    @PostLoad
    private void calcularPuntosBloqueados() {
        this.lines = Arrays.asList(startNode, endNode);
        this.blockagePoints = precomputePoints(this.lines);
    }

    public boolean isActive(LocalDateTime currentTime) {
        return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
    }

    public boolean posicionEstaBloqueada(Position posicion, LocalDateTime momento) {
        return isActive(momento) && blockagePoints != null && blockagePoints.contains(posicion);
    }

    private static Set<Position> precomputePoints(List<Position> tramos) {
        if (tramos == null || tramos.size() < 2) {
            return Collections.emptySet();
        }

        Set<Position> puntos = new HashSet<>();
        for (int i = 0; i < tramos.size() - 1; i++) {
            Position p1 = tramos.get(i);
            Position p2 = tramos.get(i + 1);

            if (p1.getY() == p2.getY()) { // Horizontal
                for (int x = Math.min(p1.getX(), p2.getX()); x <= Math.max(p1.getX(), p2.getX()); x++) {
                    puntos.add(new Position(x, p1.getY()));
                }
            } else if (p1.getX() == p2.getX()) { // Vertical
                for (int y = Math.min(p1.getY(), p2.getY()); y <= Math.max(p1.getY(), p2.getY()); y++) {
                    puntos.add(new Position(p1.getX(), y));
                }
            }
        }
        return Collections.unmodifiableSet(puntos);
    }

    @Override
    public String toString() {
        return "Blockage{" +
                "startNode=" + startNode +
                ", endNode=" + endNode +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }

    // Getters necesarios para acceso desde otras clases
    public Position getStartNode() {
        return startNode;
    }

    public Position getEndNode() {
        return endNode;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Set<Position> getBlockagePoints() {
        return blockagePoints;
    }

    public boolean isActiveAt(LocalDateTime dateTime) {
    return dateTime.isAfter(this.getStartTime()) && dateTime.isBefore(this.getEndTime());
}

}
