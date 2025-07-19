package com.example.plgsystem.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import jakarta.persistence.*;
import java.io.Serializable;

import com.example.plgsystem.enums.DepotType;

@Entity
@Table(name = "depots")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Depot implements Serializable {
    @Id
    private String id;

    @Embedded
    private Position position;

    @Column(name = "glp_capacity_m3", nullable = false)
    private int glpCapacityM3;

    @Column(name = "type", nullable = false)
    private DepotType type;

    @Column(name = "current_glp_m3", nullable = false)
    private int currentGlpM3;

    public Depot(String id, Position position, int glpCapacityM3, DepotType type) {
        this.id = id;
        this.position = position;
        this.glpCapacityM3 = glpCapacityM3;
        this.type = type;
        this.currentGlpM3 = 0;
    }

    public boolean isMain() {
        return type == DepotType.MAIN;
    }

    public boolean isAuxiliary() {
        return type == DepotType.AUXILIARY;
    }

    public boolean canServe(int glpVolumeM3) {
        return currentGlpM3 >= glpVolumeM3;
    }

    public void serve(int glpVolumeM3) {
        if (isMain()) {
            return;
        }
        currentGlpM3 = Math.max(0, currentGlpM3 - Math.abs(glpVolumeM3));
    }

    public void refill() {
        currentGlpM3 = glpCapacityM3;
    }

    public Depot copy() {
        Depot copy = new Depot(this.id, this.position != null ? this.position.clone() : null, 
                               this.glpCapacityM3, this.type);
        copy.currentGlpM3 = this.currentGlpM3;
        return copy;
    }
}
