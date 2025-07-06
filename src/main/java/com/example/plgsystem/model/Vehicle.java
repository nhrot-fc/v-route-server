package com.example.plgsystem.model;

import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un vehículo de transporte de GLP en el sistema
 */
@Entity
@Table(name = "vehicles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vehicle implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType type;
    
    @Column(name = "glp_capacity_m3", nullable = false)
    private int glpCapacityM3;
    
    @Column(name = "fuel_capacity_gal", nullable = false)
    private double fuelCapacityGal;
    
    @Embedded
    private Position currentPosition;
    
    @Column(name = "current_glp_m3", nullable = false)
    private int currentGlpM3;
    
    @Column(name = "current_fuel_gal", nullable = false)
    private double currentFuelGal;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;
    
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Incident> incidents = new ArrayList<>();
    
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServeRecord> serveRecords = new ArrayList<>();
    
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Maintenance> maintenances = new ArrayList<>();
    
    /**
     * Constructor principal para crear un nuevo vehículo
     */
    @Builder
    public Vehicle(String id, VehicleType type, Position currentPosition) {
        this.id = id;
        this.type = type;
        this.glpCapacityM3 = type.getCapacityM3();
        this.fuelCapacityGal = Constants.VEHICLE_FUEL_CAPACITY_GAL;
        this.currentPosition = currentPosition;
        this.currentGlpM3 = 0;
        this.currentFuelGal = this.fuelCapacityGal;
        this.status = VehicleStatus.AVAILABLE;
    }
    
    /**
     * Establece la posición actual del vehículo
     */
    public void setCurrentPosition(Position position) {
        this.currentPosition = position;
    }

    /**
     * Establece el estado del vehículo
     */
    public void setStatus(VehicleStatus status) {
        this.status = status;
    }
    
    /**
     * Consume combustible basado en la distancia recorrida y el peso transportado
     */
    public void consumeFuel(double distanceKm) {
        double combinedWeight = this.type.convertGlpM3ToTon(this.currentGlpM3) + this.type.getTareWeightTon();
        double fuelConsumedGallons = Math.abs((distanceKm * combinedWeight) / Constants.CONSUMPTION_FACTOR);
        this.currentFuelGal = Math.max(0, this.currentFuelGal - fuelConsumedGallons);
    }

    /**
     * Calcula el combustible necesario para recorrer una distancia determinada
     */
    public double calculateFuelNeeded(double distanceKm) {
        double combinedWeight = this.type.convertGlpM3ToTon(this.currentGlpM3)
                + this.type.getTareWeightTon();
        return Math.abs((distanceKm * combinedWeight) / Constants.CONSUMPTION_FACTOR);
    }

    /**
     * Reabastece completamente el tanque de combustible
     */
    public void refuel() {
        this.currentFuelGal = this.fuelCapacityGal;
    }

    /**
     * Dispensa GLP al cliente
     */
    public void dispenseGlp(int glpVolumeM3) {
        this.currentGlpM3 = Math.max(0, this.currentGlpM3 - Math.abs(glpVolumeM3));
    }

    /**
     * Verifica si el vehículo puede dispensar un determinado volumen de GLP
     */
    public boolean canDispenseGLP(int glpVolumeM3) {
        return this.currentGlpM3 >= Math.abs(glpVolumeM3);
    }

    /**
     * Recarga GLP en el vehículo
     */
    public void refill(int glpVolumeM3) {
        this.currentGlpM3 = Math.min(this.glpCapacityM3, this.currentGlpM3 + Math.abs(glpVolumeM3));
    }

    /**
     * Realiza una entrega a un pedido
     */
    @Transactional
    public ServeRecord serveOrder(Order order, int glpVolumeM3, LocalDateTime serveDate) {
        int absoluteVolume = Math.abs(glpVolumeM3);
        this.dispenseGlp(absoluteVolume);
        return order.recordDelivery(absoluteVolume, this, serveDate);
    }
    
    /**
     * Agrega un incidente al vehículo
     */
    public void addIncident(Incident incident) {
        incidents.add(incident);
    }
    
    /**
     * Agrega un mantenimiento al vehículo
     */
    public void addMaintenance(Maintenance maintenance) {
        maintenances.add(maintenance);
    }
    
    /**
     * Crea una copia del vehículo
     */
    public Vehicle clone() {
        Vehicle clonedVehicle = Vehicle.builder()
                .id(this.id)
                .type(this.type)
                .currentPosition(this.currentPosition.clone())
                .build();
        
        clonedVehicle.currentGlpM3 = this.currentGlpM3;
        clonedVehicle.currentFuelGal = this.currentFuelGal;
        clonedVehicle.status = this.status;
        
        return clonedVehicle;
    }

    @Override
    public String toString() {
        return String.format("🚛 %s-%s %s [🛢️ %d/%d m³][⛽ %.1f/%.1f gal] %s",
                type.name(),
                id,
                status.getIcon(),
                currentGlpM3,
                glpCapacityM3,
                currentFuelGal,
                fuelCapacityGal,
                currentPosition.toString());
    }
}
