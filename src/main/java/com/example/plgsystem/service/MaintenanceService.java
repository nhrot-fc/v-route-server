package com.example.plgsystem.service;

import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.repository.MaintenanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para la gestión de mantenimientos
 */
@Service
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;

    public MaintenanceService(MaintenanceRepository maintenanceRepository) {
        this.maintenanceRepository = maintenanceRepository;
    }
    
    /**
     * Crea un nuevo mantenimiento
     */
    @Transactional
    public Maintenance createMaintenance(String vehicleId, LocalDate assignedDate) {
        Maintenance maintenance = new Maintenance(vehicleId, assignedDate);
        return maintenanceRepository.save(maintenance);
    }
    
    /**
     * Actualiza el tiempo de inicio real del mantenimiento
     */
    @Transactional
    public Maintenance startMaintenance(Long id, LocalDateTime startTime) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found"));
        maintenance.setRealStart(startTime);
        return maintenanceRepository.save(maintenance);
    }
    
    /**
     * Actualiza el tiempo de finalización real del mantenimiento
     */
    @Transactional
    public Maintenance completeMaintenance(Long id, LocalDateTime endTime) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found"));
        maintenance.setRealEnd(endTime);
        return maintenanceRepository.save(maintenance);
    }
    
    /**
     * Encuentra todos los mantenimientos
     */
    public List<Maintenance> findAll() {
        return maintenanceRepository.findAll();
    }
    
    /**
     * Encuentra mantenimientos por ID de vehículo
     */
    public List<Maintenance> findByVehicleId(String vehicleId) {
        return maintenanceRepository.findByVehicleId(vehicleId);
    }
    
    /**
     * Encuentra mantenimientos por fecha asignada
     */
    public List<Maintenance> findByDate(LocalDate date) {
        return maintenanceRepository.findByAssignedDate(date);
    }
    
    /**
     * Encuentra mantenimientos por vehículo y fecha
     */
    public List<Maintenance> findByVehicleIdAndDate(String vehicleId, LocalDate date) {
        return maintenanceRepository.findByVehicleIdAndAssignedDate(vehicleId, date);
    }
    
    /**
     * Encuentra mantenimientos en un rango de fechas
     */
    public List<Maintenance> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return maintenanceRepository.findByAssignedDateBetween(startDate, endDate);
    }
    
    /**
     * Encuentra mantenimientos activos en el momento actual
     */
    public List<Maintenance> findActiveMaintenances() {
        return maintenanceRepository.findActiveMaintenances(LocalDateTime.now());
    }
}
