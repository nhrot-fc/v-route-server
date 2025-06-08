package com.example.plgsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "blockages")
@Getter
@NoArgsConstructor
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

    // Constructor without ID (since it's auto-generated)
    public Blockage(Position startNode, Position endNode, LocalDateTime startTime, LocalDateTime endTime) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean isActive(LocalDateTime currentTime) {
        return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
    }

    @Override
    public String toString() {
        return "BlockedStreetSegment{" +
               "startNode=" + startNode +
               ", endNode=" + endNode +
               ", startTime=" + startTime +
               ", endTime=" + endTime +
               '}';
    }
} 