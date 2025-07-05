package com.example.plgsystem.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Representa un depósito o almacén de GLP en el sistema
 */
@Entity
@Table(name = "depots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Depot implements Stop, Serializable {
    
    @Id
    private String id;
    
    @Embedded
    private Position position;
    
    @Column(name = "glp_capacity_m3", nullable = false)
    private int glpCapacityM3;
    
    @Column(name = "can_refuel", nullable = false)
    private boolean canRefuel;
    
    @Column(name = "current_glp_m3", nullable = false)
    private int currentGlpM3;
    
    /**
     * Constructor principal para crear un depósito
     */
    public Depot(String id, Position position, int glpCapacityM3, boolean canRefuel) {
        this.id = id;
        this.position = position;
        this.glpCapacityM3 = glpCapacityM3;
        this.canRefuel = canRefuel;
        this.currentGlpM3 = 0;
    }
    
    /**
     * Rellena completamente el depósito con GLP
     */
    public void refillGLP() {
        this.currentGlpM3 = glpCapacityM3;
    }
    
    /**
     * Extrae una cantidad de GLP del depósito
     * 
     * @param amountM3 Cantidad de GLP a extraer en metros cúbicos
     */
    public void serveGLP(int amountM3) {
        this.currentGlpM3 -= Math.abs(amountM3);
        this.currentGlpM3 = Math.max(0, this.currentGlpM3); // Asegura que no sea negativo
    }
    
    /**
     * Verifica si el depósito puede servir una cantidad específica de GLP
     * 
     * @param amountM3 Cantidad de GLP requerida
     * @return true si puede servir la cantidad, false en caso contrario
     */
    public boolean canServeGLP(int amountM3) {
        return currentGlpM3 >= amountM3;
    }
    
    /**
     * Establece un nuevo nivel de GLP para el depósito
     * 
     * @param amountM3 Nueva cantidad de GLP
     */
    public void setCurrentGlpM3(int amountM3) {
        this.currentGlpM3 = Math.max(0, Math.min(glpCapacityM3, amountM3));
    }
    
    @Override
    public Position getPosition() {
        return position;
    }
    
    @Override
    public String toString() {
        String refuelIcon = canRefuel ? "⛽" : "";
        return String.format("🏭 %s %s [GLP: %d/%d m³] %s", 
                id, 
                refuelIcon,
                currentGlpM3, 
                glpCapacityM3, 
                position);
    }
    
    /**
     * Crea una copia del depósito
     * @return Copia del depósito con los mismos valores
     */
    public Depot clone() {
        Depot clonedDepot = new Depot(this.id, this.position, this.glpCapacityM3, this.canRefuel);
        clonedDepot.currentGlpM3 = this.currentGlpM3;
        return clonedDepot;
    }
}
