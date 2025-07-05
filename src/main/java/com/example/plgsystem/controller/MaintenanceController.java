package com.example.plgsystem.controller;

import com.example.plgsystem.dto.MaintenanceCreateDTO;
import com.example.plgsystem.dto.MaintenanceDTO;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.service.MaintenanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de mantenimientos
 */
@RestController
@RequestMapping("/api/maintenances")
public class MaintenanceController {
    
    private final MaintenanceService maintenanceService;
    
    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }
    
    /**
     * Crea un nuevo mantenimiento
     * @param createDTO DTO con los datos para crear el mantenimiento
     * @return El mantenimiento creado
     */
    @PostMapping
    public ResponseEntity<MaintenanceDTO> createMaintenance(@RequestBody MaintenanceCreateDTO createDTO) {
        Maintenance maintenance = maintenanceService.createMaintenance(
                createDTO.getVehicleId(), 
                createDTO.getAssignedDate()
        );
        
        return new ResponseEntity<>(MaintenanceDTO.fromEntity(maintenance), HttpStatus.CREATED);
    }
    
    /**
     * Lista todos los mantenimientos con filtros opcionales de vehículo y fecha
     * @param vehicleId Filtro opcional por ID de vehículo
     * @param date Filtro opcional por fecha
     * @param startDate Filtro opcional por fecha de inicio
     * @param endDate Filtro opcional por fecha de fin
     * @return Lista de mantenimientos que cumplen con los filtros
     */
    @GetMapping
    public ResponseEntity<List<MaintenanceDTO>> listMaintenances(
            @RequestParam(required = false) String vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<Maintenance> maintenances;
        
        // Filtrado por vehículo y fecha
        if (vehicleId != null && date != null) {
            maintenances = maintenanceService.findByVehicleIdAndDate(vehicleId, date);
        }
        // Filtrado solo por vehículo
        else if (vehicleId != null) {
            maintenances = maintenanceService.findByVehicleId(vehicleId);
        }
        // Filtrado solo por fecha
        else if (date != null) {
            maintenances = maintenanceService.findByDate(date);
        }
        // Filtrado por rango de fechas
        else if (startDate != null && endDate != null) {
            maintenances = maintenanceService.findByDateRange(startDate, endDate);
        }
        // Sin filtros, retorna todos
        else {
            maintenances = maintenanceService.findAll();
        }
        
        List<MaintenanceDTO> maintenanceDTOs = maintenances.stream()
                .map(MaintenanceDTO::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(maintenanceDTOs);
    }
    
    /**
     * Lista los mantenimientos actualmente en progreso
     */
    @GetMapping("/active")
    public ResponseEntity<List<MaintenanceDTO>> listActiveMaintenances() {
        List<Maintenance> activeMaintenances = maintenanceService.findActiveMaintenances();
        
        List<MaintenanceDTO> maintenanceDTOs = activeMaintenances.stream()
                .map(MaintenanceDTO::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(maintenanceDTOs);
    }
}
