package com.example.plgsystem.service;

import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.repository.MaintenanceRepository;
import com.example.plgsystem.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para la gestión de mantenimientos
 */
@Service
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;

    public MaintenanceService(MaintenanceRepository maintenanceRepository, VehicleRepository vehicleRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.vehicleRepository = vehicleRepository;
    }
    
    /**
     * Crea un nuevo mantenimiento
     */
    @Transactional
    public Optional<Maintenance> createMaintenance(String vehicleId, LocalDate assignedDate) {
        return vehicleRepository.findById(vehicleId)
                .map(vehicle -> {
                    Maintenance maintenance = new Maintenance(vehicle, assignedDate);
                    return maintenanceRepository.save(maintenance);
                });
    }
    
    /**
     * Actualiza el tiempo de inicio real del mantenimiento
     */
    @Transactional
    public Optional<Maintenance> startMaintenance(UUID id, LocalDateTime startTime) {
        return maintenanceRepository.findById(id)
                .map(maintenance -> {
                    maintenance.setRealStart(startTime);
                    return maintenanceRepository.save(maintenance);
                });
    }
    
    /**
     * Actualiza el tiempo de finalización real del mantenimiento
     */
    @Transactional
    public Optional<Maintenance> completeMaintenance(UUID id, LocalDateTime endTime) {
        return maintenanceRepository.findById(id)
                .map(maintenance -> {
                    maintenance.setRealEnd(endTime);
                    return maintenanceRepository.save(maintenance);
                });
    }
    
    /**
     * Crea una tarea de mantenimiento de seguimiento dos meses después de un mantenimiento completado
     */
    @Transactional
    public Optional<Maintenance> createFollowUpMaintenance(UUID completedMaintenanceId) {
        return maintenanceRepository.findById(completedMaintenanceId)
                .filter(maintenance -> maintenance.getRealEnd() != null)
                .map(maintenance -> {
                    Maintenance followUp = maintenance.createNextTask();
                    if (followUp != null) {
                        return maintenanceRepository.save(followUp);
                    }
                    return null;
                });
    }
    
    /**
     * Encuentra un mantenimiento por ID
     */
    public Optional<Maintenance> findById(UUID id) {
        return maintenanceRepository.findById(id);
    }
    
    /**
     * Encuentra todos los mantenimientos
     */
    public List<Maintenance> findAll() {
        return maintenanceRepository.findAll();
    }
    
    /**
     * Encuentra todos los mantenimientos (paginado)
     */
    public Page<Maintenance> findAllPaged(Pageable pageable) {
        return maintenanceRepository.findAll(pageable);
    }
    
    /**
     * Encuentra mantenimientos por ID de vehículo
     */
    public List<Maintenance> findByVehicleId(String vehicleId) {
        return maintenanceRepository.findByVehicleId(vehicleId);
    }
    
    /**
     * Encuentra mantenimientos por ID de vehículo (paginado)
     */
    public Page<Maintenance> findByVehicleIdPaged(String vehicleId, Pageable pageable) {
        return maintenanceRepository.findByVehicleId(vehicleId, pageable);
    }
    
    /**
     * Encuentra mantenimientos por fecha asignada
     */
    public List<Maintenance> findByDate(LocalDate date) {
        return maintenanceRepository.findByAssignedDate(date);
    }
    
    /**
     * Encuentra mantenimientos por fecha asignada (paginado)
     */
    public Page<Maintenance> findByDatePaged(LocalDate date, Pageable pageable) {
        return maintenanceRepository.findByAssignedDate(date, pageable);
    }
    
    /**
     * Encuentra mantenimientos por vehículo y fecha
     */
    public List<Maintenance> findByVehicleIdAndDate(String vehicleId, LocalDate date) {
        return maintenanceRepository.findByVehicleIdAndAssignedDate(vehicleId, date);
    }
    
    /**
     * Encuentra mantenimientos por vehículo y fecha (paginado)
     */
    public Page<Maintenance> findByVehicleIdAndDatePaged(String vehicleId, LocalDate date, Pageable pageable) {
        return maintenanceRepository.findByVehicleIdAndAssignedDate(vehicleId, date, pageable);
    }
    
    /**
     * Encuentra mantenimientos en un rango de fechas
     */
    public List<Maintenance> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return maintenanceRepository.findByAssignedDateBetween(startDate, endDate);
    }
    
    /**
     * Encuentra mantenimientos en un rango de fechas (paginado)
     */
    public Page<Maintenance> findByDateRangePaged(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return maintenanceRepository.findByAssignedDateBetween(startDate, endDate, pageable);
    }
    
    /**
     * Encuentra mantenimientos activos en el momento actual
     */
    public List<Maintenance> findActiveMaintenances() {
        return maintenanceRepository.findActiveMaintenances(LocalDateTime.now());
    }
    
    /**
     * Encuentra mantenimientos activos en el momento actual (paginado)
     */
    public Page<Maintenance> findActiveMaintenancesPaged(Pageable pageable) {
        return maintenanceRepository.findActiveMaintenancesPaged(LocalDateTime.now(), pageable);
    }
}
