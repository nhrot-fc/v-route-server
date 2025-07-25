package com.example.plgsystem.service;

import com.example.plgsystem.dto.IncidentCreateDTO;
import com.example.plgsystem.dto.SimulationDTO;
import com.example.plgsystem.dto.SimulationStateDTO;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.Maintenance;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.operation.ActionType;
import com.example.plgsystem.orchest.DatabaseDataLoader;
import com.example.plgsystem.orchest.DataLoader;
import com.example.plgsystem.orchest.Event;
import com.example.plgsystem.orchest.EventType;
import com.example.plgsystem.orchest.FileDataLoader;
import com.example.plgsystem.repository.BlockageRepository;
import com.example.plgsystem.repository.DepotRepository;
import com.example.plgsystem.repository.IncidentRepository;
import com.example.plgsystem.repository.MaintenanceRepository;
import com.example.plgsystem.repository.OrderRepository;
import com.example.plgsystem.repository.VehicleRepository;
import com.example.plgsystem.simulation.Simulation;
import com.example.plgsystem.simulation.SimulationState;
import com.example.plgsystem.util.FileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;

@Service
public class SimulationService implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);

    // --------------------------------------------------------------------------
    // Fields
    // --------------------------------------------------------------------------

    private final Map<UUID, Simulation> simulations = new ConcurrentHashMap<>();
    private UUID dailyOperationsId;
    private final AtomicBoolean dailyOperationsProcessing = new AtomicBoolean(false);

    private final DepotService depotService;
    private final VehicleService vehicleService;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;
    private final BlockageRepository blockageRepository;
    private final VehicleRepository vehicleRepository;
    private final DepotRepository depotRepository;
    private final IncidentRepository incidentRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final DatabaseInitializationService databaseInitializationService;
    // --------------------------------------------------------------------------
    // Constructor
    // --------------------------------------------------------------------------

    public SimulationService(
            DepotService depotService,
            VehicleService vehicleService,
            @Lazy SimpMessagingTemplate messagingTemplate,
            OrderRepository orderRepository,
            BlockageRepository blockageRepository,
            VehicleRepository vehicleRepository,
            DepotRepository depotRepository,
            IncidentRepository incidentRepository,
            MaintenanceRepository maintenanceRepository,
            DatabaseInitializationService databaseInitializationService) {
        this.depotService = depotService;
        this.vehicleService = vehicleService;
        this.messagingTemplate = messagingTemplate;
        this.orderRepository = orderRepository;
        this.blockageRepository = blockageRepository;
        this.vehicleRepository = vehicleRepository;
        this.depotRepository = depotRepository;
        this.incidentRepository = incidentRepository;
        this.maintenanceRepository = maintenanceRepository;
        logger.info("SimulationService initialized");
        this.databaseInitializationService = databaseInitializationService;
    }

    // --------------------------------------------------------------------------
    // Lifecycle Methods
    // --------------------------------------------------------------------------

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        logger.info("Application context refreshed, initializing simulations");
        // databaseInitializationService.initializeDatabase();
        // initializeDailyOperations();
        logger.info("Simulations initialization complete");
    }

    /**
     * Limpia los recursos cuando se cierra la aplicación
     */
    public void cleanup() {
        logger.info("Limpiando recursos de simulación");

        // Apagar todos los orquestadores de simulaciones activas
        simulations.values().forEach(simulation -> {
            try {
                simulation.getOrchestrator().shutdown();
                logger.debug("Orquestador apagado para simulación: {}", simulation.getId());
            } catch (Exception e) {
                logger.error("Error al apagar orquestador de simulación {}: {}",
                        simulation.getId(), e.getMessage());
            }
        });

        FileUtils.cleanupTempFiles();
    }

    // --------------------------------------------------------------------------
    // Simulation Management - Core Methods
    // --------------------------------------------------------------------------

    /**
     * Retrieve all simulations
     */
    public Map<UUID, Simulation> getAllSimulations() {
        logger.debug("Getting all simulations, count: {}", simulations.size());
        return simulations;
    }

    /**
     * Get a specific simulation by ID
     */
    public Simulation getSimulation(UUID id) {
        logger.debug("Getting simulation with ID: {}", id);
        return simulations.get(id);
    }

    /**
     * Start a simulation by ID
     */
    public Simulation startSimulation(UUID id) {
        logger.info("Starting/resuming simulation with ID: {}", id);
        Simulation simulation = simulations.get(id);
        if (simulation != null) {
            simulation.start();
            logger.info("Simulation {} started successfully", id);
            sendSimulationUpdate(simulation);
        } else {
            logger.warn("Cannot start simulation: ID {} not found", id);
        }
        return simulation;
    }

    /**
     * Pause a simulation by ID
     */
    public Simulation pauseSimulation(UUID id) {
        logger.info("Pausing simulation with ID: {}", id);
        Simulation simulation = simulations.get(id);
        if (simulation != null) {
            simulation.pause();
            logger.info("Simulation {} paused successfully", id);
            sendSimulationUpdate(simulation);
        } else {
            logger.warn("Cannot pause simulation: ID {} not found", id);
        }
        return simulation;
    }

    /**
     * Finish a simulation by ID
     */
    public Simulation finishSimulation(UUID id) {
        logger.info("Finishing simulation with ID: {}", id);
        Simulation simulation = simulations.get(id);
        if (simulation != null) {
            simulation.finish();
            simulation.getOrchestrator().shutdown();
            logger.info("Simulation {} finished successfully", id);
            sendSimulationUpdate(simulation);
        } else {
            logger.warn("Cannot finish simulation: ID {} not found", id);
        }
        return simulation;
    }

    /**
     * Delete a simulation by ID
     */
    public void deleteSimulation(UUID id) {
        Simulation simulation = simulations.get(id);
        if (simulation != null) {
            simulation.getOrchestrator().shutdown();
            logger.info("Shutdown orchestrator resources for simulation {}", id);
        }
        simulations.remove(id);
    }

    // --------------------------------------------------------------------------
    // Simulation Creation & Initialization
    // --------------------------------------------------------------------------

    private void initializeDailyOperations() {
        logger.info("Initializing daily operations simulation");

        // Get depots and vehicles from the repository - they should already be
        // initialized
        // by the DatabaseInitializationService
        Depot mainDepot = depotService.findMainDepots().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Main depot not found"));

        List<Vehicle> vehicles = vehicleService.findAll();
        logger.info("Found {} vehicles for daily operations", vehicles.size());

        if (vehicles.isEmpty()) {
            throw new IllegalStateException("No vehicles found in the database");
        }

        List<Depot> auxDepots = depotService.findAuxiliaryDepots();
        if (auxDepots.isEmpty()) {
            throw new IllegalStateException("No auxiliary depots found in the database");
        }

        logger.info("Found {} auxiliary depots for daily operations", auxDepots.size());

        // Calculate future maintenance schedule for each vehicle
        Map<String, LocalDateTime> maintenanceSchedule = calculateMaintenanceSchedule();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, LocalDateTime> entry : maintenanceSchedule.entrySet()) {
            sb.append(entry.getKey()).append(":");
            sb.append(entry.getValue().format(DateTimeFormatter.ofPattern("yyyyMMdd"))).append("\n");
        }
        logger.info("Maintenance schedule: \n{}", sb.toString());

        SimulationState state = new SimulationState(vehicles, maintenanceSchedule, mainDepot, auxDepots,
                LocalDateTime.now());
        DataLoader dataLoader = new DatabaseDataLoader(orderRepository, blockageRepository);
        Simulation dailyOps = new Simulation(state, SimulationType.DAILY_OPERATIONS, dataLoader);
        dailyOps.start();
        dailyOperationsId = dailyOps.getId();
        simulations.put(dailyOperationsId, dailyOps);
        logger.info("Daily operations simulation created with ID: {}", dailyOperationsId);
        sendSimulationUpdate(dailyOps);
    }

    /**
     * Calculates future maintenance schedules for all vehicles based on their
     * maintenance history
     * 
     * @return A map of vehicle IDs to their next scheduled maintenance date
     */
    private Map<String, LocalDateTime> calculateMaintenanceSchedule() {
        logger.info("Calculating maintenance schedule for all vehicles");
        Map<String, LocalDateTime> maintenanceSchedule = new HashMap<>();

        // Get all vehicles
        List<Vehicle> vehicles = vehicleService.findAll();

        // For each vehicle, find their most recent maintenance and schedule next one
        int dayCount = 0;
        for (Vehicle vehicle : vehicles) {
            String vehicleId = vehicle.getId();
            List<Maintenance> vehicleMaintenances = maintenanceRepository.findByVehicleId(vehicleId);

            if (vehicleMaintenances.isEmpty()) {
                // If no previous maintenance, schedule one for two months from now
                maintenanceSchedule.put(vehicleId, LocalDateTime.now().plusMonths(2));
                logger.debug("No maintenance history for vehicle {}, scheduled for 2 months from now", vehicleId);
                continue;
            }

            // Find the most recent maintenance with a real end date
            LocalDate latestAssignedDate = null;

            for (Maintenance maintenance : vehicleMaintenances) {
                latestAssignedDate = maintenance.getAssignedDate();
            }

            LocalDateTime nextMaintenanceDate;
            if (latestAssignedDate != null) {
                nextMaintenanceDate = latestAssignedDate.atStartOfDay();
                while (nextMaintenanceDate.isBefore(LocalDateTime.now())) {
                    nextMaintenanceDate = nextMaintenanceDate.plusMonths(2);
                }
                logger.debug("Vehicle {} has maintenance assigned on {}, next scheduled for {}",
                        vehicleId, latestAssignedDate, nextMaintenanceDate);
            } else {
                // Fallback, should not happen
                nextMaintenanceDate = LocalDateTime.now().plusDays(dayCount);
                logger.warn("Unexpected condition: Vehicle {} has maintenance records but no dates found", vehicleId);
            }

            maintenanceSchedule.put(vehicleId, nextMaintenanceDate);
            dayCount += 2;
        }

        logger.info("Maintenance schedule calculated for {} vehicles", maintenanceSchedule.size());
        return maintenanceSchedule;
    }

    /**
     * Create a custom simulation with specified parameters
     */
    public Simulation createSimulation(
            SimulationType type,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int taVehicleCount,
            int tbVehicleCount,
            int tcVehicleCount,
            int tdVehicleCount) {

        logger.info(
                "Creating simplified simulation - type: {}, start: {}, end: {}, vehicles: TA={}, TB={}, TC={}, TD={}",
                type, startDateTime, endDateTime, taVehicleCount, tbVehicleCount, tcVehicleCount, tdVehicleCount);

        if (type.isDailyOperation()) {
            logger.error("Cannot create additional daily operation simulations");
            throw new IllegalArgumentException("Cannot create additional daily operation simulations");
        }

        // For WEEKLY type, set end date automatically to one week after start
        if (type == SimulationType.WEEKLY) {
            endDateTime = startDateTime.plusWeeks(1);
            logger.info("Weekly simulation: auto-set end date to {}", endDateTime);
        }

        // Get fixed depots from the database
        Depot mainDepot = new Depot("MAIN", Constants.MAIN_DEPOT_LOCATION, 10000, DepotType.MAIN);
        Depot northDepot = new Depot("NORTH", Constants.NORTH_DEPOT_LOCATION, 160, DepotType.AUXILIARY);
        Depot eastDepot = new Depot("EAST", Constants.EAST_DEPOT_LOCATION, 160, DepotType.AUXILIARY);
        List<Depot> auxDepots = Arrays.asList(northDepot, eastDepot);
        logger.info("Using fixed depots: Main={}, Aux=[{}, {}]", mainDepot.getId(), northDepot.getId(),
                eastDepot.getId());

        // Create vehicles automatically
        List<Vehicle> vehicles = new ArrayList<>();

        // Generate TA vehicles
        for (int i = 0; i < taVehicleCount; i++) {
            String id = "TA" + String.format("%02d", i + 1);
            Vehicle vehicle = Vehicle.builder()
                    .id(id)
                    .type(VehicleType.TA)
                    .currentPosition(mainDepot.getPosition().clone())
                    .build();
            vehicles.add(vehicle);
        }

        // Generate TB vehicles
        for (int i = 0; i < tbVehicleCount; i++) {
            String id = "TB" + String.format("%02d", i + 1);
            Vehicle vehicle = Vehicle.builder()
                    .id(id)
                    .type(VehicleType.TB)
                    .currentPosition(mainDepot.getPosition().clone())
                    .build();
            vehicles.add(vehicle);
        }

        // Generate TC vehicles
        for (int i = 0; i < tcVehicleCount; i++) {
            String id = "TC" + String.format("%02d", i + 1);
            Vehicle vehicle = Vehicle.builder()
                    .id(id)
                    .type(VehicleType.TC)
                    .currentPosition(mainDepot.getPosition().clone())
                    .build();
            vehicles.add(vehicle);
        }

        // Generate TD vehicles
        for (int i = 0; i < tdVehicleCount; i++) {
            String id = "TD" + String.format("%02d", i + 1);
            Vehicle vehicle = Vehicle.builder()
                    .id(id)
                    .type(VehicleType.TD)
                    .currentPosition(mainDepot.getPosition().clone())
                    .build();
            vehicles.add(vehicle);
        }

        logger.info("Created {} vehicles for simulation", vehicles.size());

        // Create maintenance schedule for custom simulation - one month from start for
        // each vehicle
        Map<String, LocalDateTime> maintenanceSchedule = new HashMap<>();
        int dayCount = 0;
        for (Vehicle vehicle : vehicles) {
            maintenanceSchedule.put(vehicle.getId(), startDateTime.plusDays(dayCount).plusMonths(2));
            dayCount += 2;
        }

        // Create simulation state
        SimulationState state = new SimulationState(vehicles, maintenanceSchedule, mainDepot, auxDepots, startDateTime);
        DataLoader dataLoader = new FileDataLoader();
        Simulation simulation = new Simulation(state, type, dataLoader);

        // Store in simulations map
        simulations.put(simulation.getId(), simulation);
        logger.info("Created simulation with ID: {}", simulation.getId());

        // Broadcast the initial state to create the channel
        sendSimulationUpdate(simulation);

        return simulation;
    }

    // --------------------------------------------------------------------------
    // Scheduled Update Methods
    // --------------------------------------------------------------------------

    @Scheduled(fixedRate = 1000)
    public void updateSimulations() {
        logger.trace("Updating other running simulations");

        // Procesar el resto de simulaciones (en memoria)
        for (Map.Entry<UUID, Simulation> entry : simulations.entrySet()) {
            // Omitir las operaciones diarias ya procesadas
            if (entry.getKey().equals(dailyOperationsId)) {
                continue;
            }

            Simulation simulation = entry.getValue();
            if (simulation.isRunning()) {
                simulation.advanceTick();
            }
        }
    }

    @Scheduled(fixedRate = 2000)
    public void updateDailyOperations() {
        // Procesar operaciones diarias de manera especial
        Simulation dailyOps = dailyOperationsId != null ? simulations.get(dailyOperationsId) : null;
        if (dailyOps != null && dailyOps.isRunning()) {
            // Solo procesar si no hay una operación en curso
            if (!dailyOperationsProcessing.getAndSet(true)) {
                try {
                    dailyOps.advanceTick();
                    // Guardar el estado en la base de datos
                    saveDailyOperationsState(dailyOps);
                } finally {
                    dailyOperationsProcessing.set(false);
                }
            }
        }
    }

    @Scheduled(fixedRate = 500)
    public void broadcastSimulationUpdates() {
        logger.trace("Broadcasting all running simulations");

        int broadcastCount = 0;
        for (Simulation simulation : simulations.values()) {
            if (simulation.isRunning()) {
                broadcastCount++;
                sendSimulationUpdate(simulation);
            }
        }

        if (broadcastCount > 0) {
            logger.debug("Broadcasted {} running simulations", broadcastCount);
        }
    }

    // --------------------------------------------------------------------------
    // Data Loading Methods
    // --------------------------------------------------------------------------

    /**
     * Carga órdenes para una simulación desde un archivo para un año/mes específico
     * 
     * @param simulation La simulación
     * @param year       Año de los datos
     * @param month      Mes de los datos
     * @param file       Archivo con datos de órdenes
     * @throws IOException Si hay problemas con el archivo
     */
    public void loadOrders(Simulation simulation, int year, int month, MultipartFile file) throws IOException {
        logger.info("Cargando órdenes para simulación: {}, año: {}, mes: {}",
                simulation.getId(), year, month);

        try {
            // Obtener DataLoader y SimulationId
            FileDataLoader fileDataLoader;

            // Verificar si el DataLoader es una instancia de FileDataLoader
            if (simulation.getOrchestrator().getDataLoader() instanceof FileDataLoader) {
                fileDataLoader = (FileDataLoader) simulation.getOrchestrator().getDataLoader();
            } else {
                logger.warn("La simulación no usa un FileDataLoader. No se pueden cargar archivos.");
                throw new IllegalStateException("Esta simulación no soporta carga de archivos");
            }

            String simulationId = simulation.getId().toString();

            // Validar y guardar el archivo
            LocalDate referenceDate = LocalDate.of(year, month, 1);
            Path filePath = FileUtils.validateOrdersFile(file, referenceDate, simulationId);

            // Registrar el archivo en DataLoader
            fileDataLoader.registerOrdersFile(year, month, filePath);
        } catch (Exception e) {
            logger.error("Error al cargar órdenes: {}", e.getMessage());
            throw new IOException("Error al cargar órdenes: " + e.getMessage(), e);
        }
    }

    /**
     * Carga bloqueos para una simulación desde un archivo para un año/mes
     * específico
     * 
     * @param simulation La simulación
     * @param year       Año de los datos
     * @param month      Mes de los datos
     * @param file       Archivo con datos de bloqueos
     * @throws IOException Si hay problemas con el archivo
     */
    public void loadBlockages(Simulation simulation, int year, int month, MultipartFile file) throws IOException {
        logger.info("Cargando bloqueos para simulación: {}, año: {}, mes: {}",
                simulation.getId(), year, month);

        try {
            // Obtener DataLoader y SimulationId
            FileDataLoader fileDataLoader;

            // Verificar si el DataLoader es una instancia de FileDataLoader
            if (simulation.getOrchestrator().getDataLoader() instanceof FileDataLoader) {
                fileDataLoader = (FileDataLoader) simulation.getOrchestrator().getDataLoader();
            } else {
                logger.warn("La simulación no usa un FileDataLoader. No se pueden cargar archivos.");
                throw new IllegalStateException("Esta simulación no soporta carga de archivos");
            }

            String simulationId = simulation.getId().toString();

            // Validar y guardar el archivo
            LocalDate referenceDate = LocalDate.of(year, month, 1);
            Path filePath = FileUtils.validateBlockagesFile(file, referenceDate, simulationId);

            // Registrar el archivo en DataLoader
            fileDataLoader.registerBlockagesFile(year, month, filePath);
        } catch (Exception e) {
            logger.error("Error al cargar bloqueos: {}", e.getMessage());
            throw new IOException("Error al cargar bloqueos: " + e.getMessage(), e);
        }
    }

    // --------------------------------------------------------------------------
    // State Management Methods
    // --------------------------------------------------------------------------

    /**
     * Guarda el estado actual de la simulación de operaciones diarias en la base de
     * datos
     */
    @Transactional
    public void saveDailyOperationsState(Simulation simulation) {
        if (!simulation.isDailyOperation()) {
            logger.warn("Intento de guardar estado de una simulación que no es de operaciones diarias");
            return;
        }

        SimulationState state = simulation.getState();

        try {
            // Actualizar vehículos
            List<Vehicle> vehicles = state.getVehicles();
            vehicleRepository.saveAll(vehicles);

            // Actualizar depósitos
            List<Depot> allDepots = new ArrayList<>();
            allDepots.add(state.getMainDepot());
            allDepots.addAll(state.getAuxDepots());
            depotRepository.saveAll(allDepots);

            // Actualizar órdenes
            List<Order> orders = state.getOrders();
            orderRepository.saveAll(orders);

            // Actualizar incidentes
            List<Incident> incidents = state.getIncidents();
            incidentRepository.saveAll(incidents);

            // Actualizar mantenimientos
            List<Maintenance> maintenances = state.getMaintenances();
            maintenanceRepository.saveAll(maintenances);

            // Actualizar bloqueos
            List<Blockage> blockages = state.getBlockages();
            blockageRepository.saveAll(blockages);
        } catch (Exception e) {
            logger.error("Error al guardar el estado de operaciones diarias: {}", e.getMessage(), e);
            simulation.error();
        }
    }

    // --------------------------------------------------------------------------
    // Event Handling Methods
    // --------------------------------------------------------------------------

    /**
     * Crea un evento de avería para un vehículo en una simulación específica
     * 
     * @param simulation  La simulación donde ocurre la avería
     * @param incidentDTO Datos de la avería a crear
     * @throws IllegalArgumentException Si el vehículo no existe en la simulación o
     *                                  hay algún error en los datos
     */
    public void createVehicleBreakdown(Simulation simulation, IncidentCreateDTO incidentDTO) {
        logger.info("Creando avería para vehículo {} en simulación {}",
                incidentDTO.getVehicleId(), simulation.getId());

        // Verificar que el vehículo existe en la simulación
        Vehicle vehicle = simulation.getState().getVehicleById(incidentDTO.getVehicleId());
        if (vehicle == null) {
            logger.error("Vehículo no encontrado: {}", incidentDTO.getVehicleId());
            throw new IllegalArgumentException("Vehículo no encontrado: " + incidentDTO.getVehicleId());
        }

        // Verificar que el tipo de avería es válido
        if (incidentDTO.getType() == null) {
            logger.error("Tipo de avería no especificado");
            throw new IllegalArgumentException("Tipo de avería no especificado");
        }

        // Si el vehículo está realizando una acción, se establece el tiempo de
        // ocurrencia en el momento de finalizar la acción
        LocalDateTime occurrenceTime = simulation.getOrchestrator().getState().getCurrentTime();
        if (vehicle.isPerformingAction() && vehicle.getCurrentAction().getType() != ActionType.DRIVE) {
            occurrenceTime = vehicle.getCurrentActionEndTime();
            logger.debug("Vehículo {} está realizando una acción, estableciendo tiempo de ocurrencia en {}",
                    vehicle.getId(), occurrenceTime);
            incidentDTO.setOccurrenceTime(occurrenceTime);
        } else {
            // Si no está realizando una acción, se usa el tiempo actual de la simulación
            incidentDTO.setOccurrenceTime(simulation.getOrchestrator().getState().getCurrentTime());
        }

        // Crear el incidente
        Incident incident = new Incident(vehicle, incidentDTO.getType(), incidentDTO.getOccurrenceTime());

        // Crear el evento de avería
        Event breakdownEvent = new Event(
                EventType.BREAKDOWN,
                incidentDTO.getOccurrenceTime(),
                vehicle.getId(),
                incident);

        // Añadir el evento al orquestador de la simulación
        simulation.getOrchestrator().addEvent(breakdownEvent);
        logger.info("Avería creada exitosamente para vehículo {} en simulación {}",
                vehicle.getId(), simulation.getId());
    }

    // --------------------------------------------------------------------------
    // Communication Methods
    // --------------------------------------------------------------------------

    /**
     * Send simulation updates via WebSocket
     */
    public void sendSimulationUpdate(Simulation simulation) {
        logger.trace("Sending WebSocket update for simulation ID: {}", simulation.getId());

        UUID id = simulation.getId();
        String channelBasePath = "/topic/simulation/" + id;

        messagingTemplate.convertAndSend(
                channelBasePath,
                new SimulationDTO(simulation));

        SimulationStateDTO stateDTO = SimulationStateDTO.fromSimulationState(
                id.toString(),
                simulation.getState(),
                simulation.getStatus());

        messagingTemplate.convertAndSend(
                channelBasePath + "/state",
                stateDTO);
    }

    /**
     * Simple getter for simulations map
     */
    public Map<UUID, Simulation> getSimulations() {
        return simulations;
    }
}