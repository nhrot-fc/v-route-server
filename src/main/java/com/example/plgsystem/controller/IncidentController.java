package com.example.plgsystem.controller;

import com.example.plgsystem.dto.IncidentCreateDTO;
import com.example.plgsystem.dto.IncidentDTO;
import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.enums.IncidentType;
import com.example.plgsystem.enums.Shift;
import com.example.plgsystem.service.IncidentService;
import com.example.plgsystem.service.VehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private static final Logger logger = LoggerFactory.getLogger(IncidentController.class);
    private final IncidentService incidentService;
    private final VehicleService vehicleService;

    public IncidentController(IncidentService incidentService, VehicleService vehicleService) {
        this.incidentService = incidentService;
        this.vehicleService = vehicleService;
        logger.info("IncidentController initialized");
    }

    /**
     * Crear un nuevo incidente
     */
    @PostMapping
    public ResponseEntity<IncidentDTO> create(@RequestBody IncidentCreateDTO createDTO) {
        logger.info("Creating new incident for vehicle ID: {}, type: {}", createDTO.getVehicleId(), createDTO.getType());
        Optional<Vehicle> vehicleOpt = vehicleService.findById(createDTO.getVehicleId());

        if (vehicleOpt.isEmpty()) {
            logger.warn("Failed to create incident: Vehicle with ID {} not found", createDTO.getVehicleId());
            return ResponseEntity.badRequest().build();
        }

        Vehicle vehicle = vehicleOpt.get();
        LocalDateTime occurrenceTime = createDTO.getOccurrenceTime() != null ? createDTO.getOccurrenceTime()
                : LocalDateTime.now();

        Incident incident = new Incident(vehicle, createDTO.getType(), occurrenceTime);

        Incident savedIncident = incidentService.save(incident);
        logger.info("Incident created with ID: {}", savedIncident.getId());
        return new ResponseEntity<>(IncidentDTO.fromEntity(savedIncident), HttpStatus.CREATED);
    }

    /**
     * Listar incidentes con opciones de filtrado y paginación opcional
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String vehicleId,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) Shift shift,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) Boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "occurrenceTime") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        logger.info("Listing incidents with filters - vehicleId: {}, type: {}, shift: {}, startDate: {}, endDate: {}, resolved: {}, paginated: {}, page: {}, size: {}, sortBy: {}, direction: {}",
                vehicleId, type, shift, startDate, endDate, resolved, paginated, page, size, sortBy, direction);

        // Si paginated es null o false, devolvemos todos los resultados sin paginar
        if (paginated == null || !paginated) {
            List<Incident> incidents;

            if (vehicleId != null && startDate != null && endDate != null) {
                logger.info("Filtering incidents by vehicle ID: {} and date range: {} to {}", vehicleId, startDate, endDate);
                incidents = incidentService.findByVehicleAndDateRange(vehicleId, startDate, endDate);
            } else if (vehicleId != null) {
                logger.info("Filtering incidents by vehicle ID: {}", vehicleId);
                incidents = incidentService.findByVehicleId(vehicleId);
            } else if (type != null) {
                logger.info("Filtering incidents by type: {}", type);
                incidents = incidentService.findByType(type);
            } else if (shift != null) {
                logger.info("Filtering incidents by shift: {}", shift);
                incidents = incidentService.findByShift(shift);
            } else if (startDate != null && endDate != null) {
                logger.info("Filtering incidents by date range: {} to {}", startDate, endDate);
                incidents = incidentService.findByDateRange(startDate, endDate);
            } else if (resolved != null) {
                logger.info("Filtering incidents by resolved status: {}", resolved);
                incidents = incidentService.findByResolved(resolved);
            } else {
                logger.info("Retrieving all incidents without filtering");
                incidents = incidentService.findAll();
            }

            List<IncidentDTO> incidentDTOs = incidents.stream()
                    .map(IncidentDTO::fromEntity)
                    .collect(Collectors.toList());

            logger.info("Found {} incidents matching criteria", incidentDTOs.size());
            return ResponseEntity.ok(incidentDTOs);
        }

        // Si paginated es true, devolvemos resultados paginados
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Incident> incidentPage;

        if (vehicleId != null && startDate != null && endDate != null) {
            logger.info("Filtering paginated incidents by vehicle ID: {} and date range: {} to {}", vehicleId, startDate, endDate);
            incidentPage = incidentService.findByVehicleAndDateRangePaged(vehicleId, startDate, endDate, pageable);
        } else if (vehicleId != null) {
            logger.info("Filtering paginated incidents by vehicle ID: {}", vehicleId);
            incidentPage = incidentService.findByVehicleIdPaged(vehicleId, pageable);
        } else if (type != null) {
            logger.info("Filtering paginated incidents by type: {}", type);
            incidentPage = incidentService.findByTypePaged(type, pageable);
        } else if (shift != null) {
            logger.info("Filtering paginated incidents by shift: {}", shift);
            incidentPage = incidentService.findByShiftPaged(shift, pageable);
        } else if (startDate != null && endDate != null) {
            logger.info("Filtering paginated incidents by date range: {} to {}", startDate, endDate);
            incidentPage = incidentService.findByDateRangePaged(startDate, endDate, pageable);
        } else if (resolved != null) {
            logger.info("Filtering paginated incidents by resolved status: {}", resolved);
            incidentPage = incidentService.findByResolvedPaged(resolved, pageable);
        } else {
            logger.info("Retrieving all paginated incidents without filtering");
            incidentPage = incidentService.findAllPaged(pageable);
        }

        logger.info("Found page {} of {} with {} incidents per page (total: {})", 
                incidentPage.getNumber(), incidentPage.getTotalPages(), incidentPage.getSize(), incidentPage.getTotalElements());
        return ResponseEntity.ok(incidentPage.map(IncidentDTO::fromEntity));
    }

    /**
     * Obtener un incidente por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncidentDTO> getById(@PathVariable UUID id) {
        logger.info("Fetching incident with ID: {}", id);
        return incidentService.findById(id)
                .map(incident -> {
                    logger.info("Incident found with ID: {}", id);
                    return ResponseEntity.ok(IncidentDTO.fromEntity(incident));
                })
                .orElseGet(() -> {
                    logger.warn("Incident with ID: {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Marcar un incidente como resuelto
     */
    @PatchMapping("/{id}/resolve")
    public ResponseEntity<Incident> resolveIncident(@PathVariable UUID id) {
        logger.info("Attempting to resolve incident with ID: {}", id);
        return incidentService.resolveIncident(id)
                .map(incident -> {
                    logger.info("Incident with ID: {} was successfully resolved", id);
                    return ResponseEntity.ok(incident);
                })
                .orElseGet(() -> {
                    logger.warn("Incident with ID: {} not found for resolution", id);
                    return ResponseEntity.notFound().build();
                });
    }
}
