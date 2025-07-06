package com.example.plgsystem.controller;

import com.example.plgsystem.dto.MaintenanceCreateDTO;
import com.example.plgsystem.dto.MaintenanceDTO;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.service.MaintenanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
     * Lista todos los mantenimientos con filtros opcionales de vehículo y fecha, con paginación opcional
     * @param vehicleId Filtro opcional por ID de vehículo
     * @param date Filtro opcional por fecha
     * @param startDate Filtro opcional por fecha de inicio
     * @param endDate Filtro opcional por fecha de fin
     * @param paginated Indica si se debe paginar el resultado (por defecto false)
     * @param page Número de página (empieza en 0)
     * @param size Tamaño de la página
     * @param sortBy Campo para ordenamiento
     * @param direction Dirección del ordenamiento (asc/desc)
     * @return Lista de mantenimientos que cumplen con los filtros
     */
    @GetMapping
    public ResponseEntity<?> listMaintenances(
            @RequestParam(required = false) String vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "assignedDate") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        
        // Si paginated es null o false, devolvemos todos los resultados sin paginar
        if (paginated == null || !paginated) {
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
        
        // Si paginated es true, devolvemos resultados paginados
        Sort sort = direction.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Maintenance> maintenancePage;
        
        // Filtrado por vehículo y fecha
        if (vehicleId != null && date != null) {
            maintenancePage = maintenanceService.findByVehicleIdAndDatePaged(vehicleId, date, pageable);
        }
        // Filtrado solo por vehículo
        else if (vehicleId != null) {
            maintenancePage = maintenanceService.findByVehicleIdPaged(vehicleId, pageable);
        }
        // Filtrado solo por fecha
        else if (date != null) {
            maintenancePage = maintenanceService.findByDatePaged(date, pageable);
        }
        // Filtrado por rango de fechas
        else if (startDate != null && endDate != null) {
            maintenancePage = maintenanceService.findByDateRangePaged(startDate, endDate, pageable);
        }
        // Sin filtros, retorna todos
        else {
            maintenancePage = maintenanceService.findAllPaged(pageable);
        }
        
        return ResponseEntity.ok(maintenancePage.map(MaintenanceDTO::fromEntity));
    }
    
    /**
     * Lista los mantenimientos actualmente en progreso, con paginación opcional
     */
    @GetMapping("/active")
    public ResponseEntity<?> listActiveMaintenances(
            @RequestParam(required = false) Boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "realStart") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        // Si paginated es null o false, devolvemos todos los resultados sin paginar
        if (paginated == null || !paginated) {
            List<Maintenance> activeMaintenances = maintenanceService.findActiveMaintenances();
            
            List<MaintenanceDTO> maintenanceDTOs = activeMaintenances.stream()
                    .map(MaintenanceDTO::fromEntity)
                    .collect(Collectors.toList());
                    
            return ResponseEntity.ok(maintenanceDTOs);
        }
        
        // Si paginated es true, devolvemos resultados paginados
        Sort sort = direction.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Maintenance> activeMaintenancesPage = maintenanceService.findActiveMaintenancesPaged(pageable);
        
        return ResponseEntity.ok(activeMaintenancesPage.map(MaintenanceDTO::fromEntity));
    }
    
    /**
     * Obtiene un mantenimiento por su ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceDTO> getMaintenanceById(@PathVariable Long id) {
        return maintenanceService.findById(id)
                .map(maintenance -> ResponseEntity.ok(MaintenanceDTO.fromEntity(maintenance)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
