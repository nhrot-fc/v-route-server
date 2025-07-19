package com.example.plgsystem.service;

import com.example.plgsystem.dto.SimulationDTO;
import com.example.plgsystem.dto.SimulationStateDTO;
import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.orchest.Event;
import com.example.plgsystem.orchest.DatabaseDataLoader;
import com.example.plgsystem.orchest.FileDataLoader;
import com.example.plgsystem.orchest.DataLoader;
import com.example.plgsystem.repository.BlockageRepository;
import com.example.plgsystem.repository.OrderRepository;
import com.example.plgsystem.simulation.Simulation;
import com.example.plgsystem.simulation.SimulationState;
import com.example.plgsystem.util.FileUtils;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.example.plgsystem.dto.IncidentCreateDTO;
import com.example.plgsystem.model.Incident;
import com.example.plgsystem.orchest.EventType;
import com.example.plgsystem.enums.VehicleStatus;

@Service
public class SimulationService implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);

    private final Map<UUID, Simulation> simulations = new ConcurrentHashMap<>();
    private UUID dailyOperationsId;

    private final DepotService depotService;
    private final VehicleService vehicleService;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;
    private final BlockageRepository blockageRepository;

    public SimulationService(
            DepotService depotService,
            VehicleService vehicleService,
            @Lazy SimpMessagingTemplate messagingTemplate,
            OrderRepository orderRepository,
            BlockageRepository blockageRepository) {
        this.depotService = depotService;
        this.vehicleService = vehicleService;
        this.messagingTemplate = messagingTemplate;
        this.orderRepository = orderRepository;
        this.blockageRepository = blockageRepository;
        logger.info("SimulationService initialized");
    }

    public Map<UUID, Simulation> getSimulations() {
        return simulations;
    }

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        logger.info("Application context refreshed, initializing daily operations");
        initializeDailyOperations();
    }

    private void initializeDailyOperations() {
        logger.info("Initializing daily operations simulation");
        Depot mainDepot = depotService.findMainDepots().stream().findFirst().orElse(null);
        if (mainDepot == null) {
            mainDepot = new Depot("MAIN", Constants.MAIN_DEPOT_LOCATION, 10000, DepotType.MAIN);
            depotService.save(mainDepot);
        }
        List<Vehicle> vehicles = vehicleService.findAll();
        logger.info("Found {} vehicles for daily operations", vehicles.size());
        if (vehicles.isEmpty()) {
            // Generate TA vehicles
            for (int i = 0; i < 2; i++) {
                String id = "TA" + String.format("%02d", i + 1);
                Vehicle vehicle = Vehicle.builder()
                        .id(id)
                        .type(VehicleType.TA)
                        .currentPosition(mainDepot.getPosition().clone())
                        .build();
                vehicles.add(vehicle);
            }

            // Generate TB vehicles
            for (int i = 0; i < 4; i++) {
                String id = "TB" + String.format("%02d", i + 1);
                Vehicle vehicle = Vehicle.builder()
                        .id(id)
                        .type(VehicleType.TB)
                        .currentPosition(mainDepot.getPosition().clone())
                        .build();
                vehicles.add(vehicle);
            }

            // Generate TC vehicles
            for (int i = 0; i < 4; i++) {
                String id = "TC" + String.format("%02d", i + 1);
                Vehicle vehicle = Vehicle.builder()
                        .id(id)
                        .type(VehicleType.TC)
                        .currentPosition(mainDepot.getPosition().clone())
                        .build();
                vehicles.add(vehicle);
            }

            // Generate TD vehicles
            for (int i = 0; i < 10; i++) {
                String id = "TD" + String.format("%02d", i + 1);
                Vehicle vehicle = Vehicle.builder()
                        .id(id)
                        .type(VehicleType.TD)
                        .currentPosition(mainDepot.getPosition().clone())
                        .build();
                vehicles.add(vehicle);
            }
        }

        vehicles.forEach(vehicleService::save);

        List<Depot> auxDepots = depotService.findAuxiliaryDepots();
        if (auxDepots.isEmpty()) {
            auxDepots = Arrays.asList(
                    new Depot("NORTH", Constants.NORTH_DEPOT_LOCATION, 500, DepotType.AUXILIARY),
                    new Depot("EAST", Constants.EAST_DEPOT_LOCATION, 500, DepotType.AUXILIARY));
            for (Depot depot : auxDepots) {
                depotService.save(depot);
            }
        }

        logger.info("Found {} auxiliary depots for daily operations", auxDepots.size());
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, LocalDateTime.now());
        DataLoader dataLoader = new DatabaseDataLoader(orderRepository, blockageRepository);
        Simulation dailyOps = new Simulation(state, SimulationType.DAILY_OPERATIONS, dataLoader);
        dailyOps.start();
        dailyOperationsId = dailyOps.getId();
        simulations.put(dailyOperationsId, dailyOps);
        logger.info("Daily operations simulation created with ID: {}", dailyOperationsId);
        sendSimulationUpdate(dailyOps);
    }

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
        Depot northDepot = new Depot("NORTH", Constants.NORTH_DEPOT_LOCATION, 500, DepotType.AUXILIARY);
        Depot eastDepot = new Depot("EAST", Constants.EAST_DEPOT_LOCATION, 500, DepotType.AUXILIARY);
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

        // Create simulation state
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, startDateTime);
        DataLoader dataLoader = new FileDataLoader();
        Simulation simulation = new Simulation(state, type, dataLoader);

        // Store in simulations map
        simulations.put(simulation.getId(), simulation);
        logger.info("Created simulation with ID: {}", simulation.getId());

        // Broadcast the initial state to create the channel
        sendSimulationUpdate(simulation);

        return simulation;
    }

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

            // Cargar órdenes para la fecha actual de simulación
            LocalDateTime simTime = simulation.getSimulationTime();
            if (simTime.getYear() == year && simTime.getMonthValue() == month) {
                List<Event> events = fileDataLoader.loadOrdersForDate(simTime.toLocalDate());
                simulation.getOrchestrator().addEvents(events);
                logger.info("Añadidos {} eventos de órdenes para la fecha {}",
                        events.size(), simTime.toLocalDate());
            }
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

            // Cargar bloqueos para la fecha actual de simulación
            LocalDateTime simTime = simulation.getSimulationTime();
            if (simTime.getYear() == year && simTime.getMonthValue() == month) {
                List<Event> events = fileDataLoader.loadBlockagesForDate(simTime.toLocalDate());
                simulation.getOrchestrator().addEvents(events);
                logger.info("Añadidos {} eventos de bloqueos para la fecha {}",
                        events.size(), simTime.toLocalDate());
            }
        } catch (Exception e) {
            logger.error("Error al cargar bloqueos: {}", e.getMessage());
            throw new IOException("Error al cargar bloqueos: " + e.getMessage(), e);
        }
    }

    public Simulation getSimulation(UUID id) {
        logger.debug("Getting simulation with ID: {}", id);
        return simulations.get(id);
    }

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

    public Simulation finishSimulation(UUID id) {
        logger.info("Finishing simulation with ID: {}", id);
        Simulation simulation = simulations.get(id);
        if (simulation != null) {
            simulation.finish();
            simulation.getOrchestrator().shutdown(); // Apagar el orchestrator y sus recursos
            logger.info("Simulation {} finished successfully", id);
            sendSimulationUpdate(simulation);
        } else {
            logger.warn("Cannot finish simulation: ID {} not found", id);
        }
        return simulation;
    }

    public Map<UUID, Simulation> getAllSimulations() {
        logger.debug("Getting all simulations, count: {}", simulations.size());
        return simulations;
    }

    @Scheduled(fixedRate = 1000)
    public void updateSimulations() {
        logger.trace("Updating all running simulations");
        for (Simulation simulation : simulations.values()) {
            if (simulation.isRunning()) {
                simulation.advanceTick();
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

    public void deleteSimulation(UUID id) {
        Simulation simulation = simulations.get(id);
        if (simulation != null) {
            simulation.getOrchestrator().shutdown();
            logger.info("Shutdown orchestrator resources for simulation {}", id);
        }
        simulations.remove(id);
    }

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

        // Crear el incidente
        Incident incident = new Incident(vehicle, incidentDTO.getType(), incidentDTO.getOccurrenceTime());

        // Crear el evento de avería
        Event breakdownEvent = new Event(
                EventType.VEHICLE_BREAKDOWN,
                incidentDTO.getOccurrenceTime(),
                vehicle.getId(),
                incident);

        // Añadir el evento al orquestador de la simulación
        simulation.getOrchestrator().addEvent(breakdownEvent);

        // También añadir el incidente directamente al estado para procesamiento
        // inmediato si el tiempo coincide
        if (incidentDTO.getOccurrenceTime().isEqual(simulation.getSimulationTime()) ||
                incidentDTO.getOccurrenceTime().isBefore(simulation.getSimulationTime())) {
            simulation.getState().addIncident(incident);
            vehicle.setStatus(VehicleStatus.INCIDENT);
            simulation.getState().getCurrentVehiclePlans().remove(vehicle.getId());
        }

        logger.info("Avería creada exitosamente para vehículo {} en simulación {}",
                vehicle.getId(), simulation.getId());
    }
}