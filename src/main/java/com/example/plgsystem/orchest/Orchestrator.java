package com.example.plgsystem.orchest;

import com.example.plgsystem.assignation.MetaheuristicSolver;
import com.example.plgsystem.assignation.Route;
import com.example.plgsystem.assignation.Solution;
import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Incident;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.operation.VehiclePlan;
import com.example.plgsystem.operation.VehiclePlanCreator;
import com.example.plgsystem.simulation.SimulationState;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import com.example.plgsystem.model.Maintenance;

@Getter
public class Orchestrator {
    private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);
    private static final int TICKS_TO_CHECK_EVENTS = 10;
    private static int FUTURE_PROJECTION_MINUTES = 90;

    private final boolean isDailyOperation;
    private final SimulationState state;
    private final PriorityQueue<Event> eventQueue;
    private final DataLoader dataLoader;

    private int ticksToCheckEvents;
    private int ticksToReplan;
    private boolean replanFlag;

    // Nuevos atributos para replanificación asíncrona
    private final ExecutorService plannerExecutor;
    private Future<?> currentPlanningTask;
    private LocalDateTime targetPlanningTime;
    private Map<String, VehiclePlan> futurePlans;
    private boolean planningInProgress;

    public Orchestrator(SimulationState state, DataLoader dataLoader, boolean isDailyOperation) {
        this.isDailyOperation = isDailyOperation;
        FUTURE_PROJECTION_MINUTES = isDailyOperation ? 90 : 5;

        this.dataLoader = dataLoader;
        this.state = state;
        this.eventQueue = new PriorityQueue<>(Event::compareTo);

        this.ticksToCheckEvents = 0;
        this.ticksToReplan = 0;
        this.replanFlag = true;

        // Inicializar componentes para planificación asíncrona
        this.plannerExecutor = Executors.newSingleThreadExecutor();
        this.futurePlans = new HashMap<>();
        this.planningInProgress = false;
        logger.debug("Orchestrator inicializado con planificación asíncrona");
    }

    public void addEvent(Event e) {
        eventQueue.add(e);
    }

    public void addEvents(List<Event> eventList) {
        eventQueue.addAll(eventList);
    }

    public void advanceTick() {
        // Update check events countDown
        ticksToCheckEvents = ticksToCheckEvents - 1;
        if (ticksToCheckEvents == 0 || isDailyOperation) {
            checkAndLoadNewEvents();
            ticksToCheckEvents = TICKS_TO_CHECK_EVENTS;
        }

        // Daily Operations a tick is from previous time to real time
        // Simulation each tick represents 1 minute on simulation
        LocalDateTime nextTickTime = isDailyOperation ? LocalDateTime.now() : state.getCurrentTime().plusMinutes(1);

        // Poll events
        pollEvents(nextTickTime);

        // Check if we need to start replanification
        checkReplanification();

        // Check if we need to apply future plans
        checkApplyFuturePlans(nextTickTime);

        // Update state
        state.advanceTime(Duration.between(state.getCurrentTime(), nextTickTime));

        // Update replan countDown
        ticksToReplan = ticksToReplan - 1;
    }

    private void checkAndLoadNewEvents() {
        logger.info("Checking for new events from data loader");
        
        // Track existing orders and blockages to avoid duplicates
        Set<String> currentOrderIds = new HashSet<>();
        Set<String> currentBlockageIds = new HashSet<>();

        // Add orders and blockages from the event queue to our tracking sets
        for (Event event : eventQueue) {
            if (event.getType() == EventType.ORDER_ARRIVAL) {
                currentOrderIds.add(event.getEntityId());
            } else if (event.getType() == EventType.BLOCKAGE_START) {
                Blockage blockage = (Blockage) event.getData();
                String blockageId = createBlockageIdentifier(blockage);
                currentBlockageIds.add(blockageId);
            }
        }

        // Add orders and blockages from the current state to our tracking sets
        for (Order order : state.getOrders()) {
            currentOrderIds.add(order.getId());
        }
        for (Blockage blockage : state.getBlockages()) {
            String blockageId = createBlockageIdentifier(blockage);
            currentBlockageIds.add(blockageId);
        }

        // Load potential new events for the current date
        List<Event> newOrderEvents = dataLoader.loadOrdersForDate(state.getCurrentTime().toLocalDate());
        List<Event> newBlockageEvents = dataLoader.loadBlockagesForDate(state.getCurrentTime().toLocalDate());

        LocalDateTime currentTime = state.getCurrentTime();
        List<Event> eventsToAdd = new ArrayList<>();
        int newEventsCount = 0;

        // Process order events
        for (Event event : newOrderEvents) {
            if (event.getType() == EventType.ORDER_ARRIVAL) {
                Order order = (Order) event.getData();
                
                // Only add if: 1) Not a duplicate and 2) Deadline is in the future
                if (!currentOrderIds.contains(order.getId()) && 
                    order.getDeadlineTime().isAfter(currentTime)) {
                    
                    eventsToAdd.add(event);
                    currentOrderIds.add(order.getId()); // Mark as processed
                    newEventsCount++;
                    logger.debug("New order event added: {} (deadline: {})", 
                        order.getId(), order.getDeadlineTime());
                }
            }
        }

        // Process blockage events
        for (Event event : newBlockageEvents) {
            if (event.getType() == EventType.BLOCKAGE_START) {
                Blockage blockage = (Blockage) event.getData();
                String blockageId = createBlockageIdentifier(blockage);
                
                // Only add if: 1) Not a duplicate and 2) End time is in the future
                if (!currentBlockageIds.contains(blockageId) && 
                    blockage.getEndTime().isAfter(currentTime)) {
                    
                    eventsToAdd.add(event);
                    currentBlockageIds.add(blockageId); // Mark as processed
                    
                    // Add the blockage to the state directly
                    state.addBlockage(blockage);
                    
                    newEventsCount++;
                    logger.debug("New blockage event added: {} to {}", 
                        blockage.getStartTime(), blockage.getEndTime());
                }
            }
        }

        // Add all filtered events to the event queue
        if (!eventsToAdd.isEmpty()) {
            eventQueue.addAll(eventsToAdd);
            logger.info("Added {} new events to event queue", newEventsCount);
        } else {
            logger.debug("No new events to add");
        }
    }

    /**
     * Creates a unique identifier for a blockage based on its properties
     */
    private String createBlockageIdentifier(Blockage blockage) {
        return String.format("%03d-%03d-%03d-%03d-%s-%s",
                (int) blockage.getLines().get(0).getX(), 
                (int) blockage.getLines().get(0).getY(),
                (int) blockage.getLines().get(1).getX(), 
                (int) blockage.getLines().get(1).getY(),
                blockage.getStartTime(), 
                blockage.getEndTime());
    }

    private void pollEvents(LocalDateTime nextTickTime) {
        while (!eventQueue.isEmpty() && eventQueue.peek().getTime().isBefore(nextTickTime)) {
            Event event = eventQueue.poll();
            processEvent(event);
        }
    }

    private void checkReplanification() {
        // Si es tiempo de replanificar o hay una bandera de replanificación activa
        if ((ticksToReplan <= 0 || replanFlag) && !planningInProgress) {
            logger.debug("Iniciando proceso de replanificación: ticksToReplan={}, replanFlag={}",
                    ticksToReplan, replanFlag);
            startAsyncReplanification();
            replanFlag = false;
            ticksToReplan = FUTURE_PROJECTION_MINUTES;
        }

        // Verificar vehículos sin plan que estén fuera de planta y enviarlos a planta
        for (Vehicle vehicle : state.getVehicles()) {
            if (!state.getCurrentVehiclePlans().containsKey(vehicle.getId()) &&
                    vehicle.isAvailable() &&
                    !isAtMainDepot(vehicle)) {

                logger.info("Vehículo {} sin plan y fuera de planta, creando plan para retorno a base",
                        vehicle.getId());
                VehiclePlan planToMainDepot = VehiclePlanCreator.createPlanToMainDepot(vehicle, state);

                if (planToMainDepot != null) {
                    state.addVehiclePlan(vehicle.getId(), planToMainDepot);
                    logger.debug("Plan de retorno a base creado para vehículo {}", vehicle.getId());
                }
            }
        }
    }

    private boolean isAtMainDepot(Vehicle vehicle) {
        if (state.getMainDepot() == null || vehicle == null ||
                state.getMainDepot().getPosition() == null ||
                vehicle.getCurrentPosition() == null) {
            return false;
        }
        return vehicle.getCurrentPosition().equals(state.getMainDepot().getPosition());
    }

    private void startAsyncReplanification() {
        SimulationState futureState = state.createSnapshot();
        LocalDateTime projectedTime = futureState.getCurrentTime().plusMinutes(FUTURE_PROJECTION_MINUTES);
        applyEventsToFutureState(futureState, projectedTime);
        futureState.advanceTime(Duration.between(futureState.getCurrentTime(), projectedTime));
        targetPlanningTime = projectedTime;

        logger.info("Iniciando replanificación asíncrona para el tiempo: {}", targetPlanningTime);

        planningInProgress = true;
        currentPlanningTask = plannerExecutor.submit(() -> {
            Thread.currentThread().setName("PlannerThread");
            logger.debug("Thread de planificación iniciado para tiempo objetivo: {}", targetPlanningTime);
            try {
                Map<String, VehiclePlan> newPlans = generateNewPlans(futureState);

                synchronized (this) {
                    futurePlans = newPlans;
                    logger.info("Replanificación completada para el tiempo: {}, generados {} planes",
                            targetPlanningTime, futurePlans.size());
                }
            } catch (Exception e) {
                logger.error("Error durante la replanificación: ", e);
            } finally {
                planningInProgress = false;
                logger.debug("Thread de planificación finalizado");
            }
        });
    }

    private void applyEventsToFutureState(SimulationState futureState, LocalDateTime projectedTime) {
        logger.debug("Aplicando eventos futuros a la proyección hasta: {}", projectedTime);

        // Creamos una copia de la cola de eventos para no modificar la original
        PriorityQueue<Event> eventsCopy = new PriorityQueue<>(eventQueue);

        // Procesamos eventos que ocurrirían antes del tiempo proyectado
        while (!eventsCopy.isEmpty() && eventsCopy.peek().getTime().isBefore(projectedTime)) {
            Event event = eventsCopy.poll();
            logger.debug("Aplicando evento futuro: {} al estado proyectado", event);

            switch (event.getType()) {
                case ORDER_ARRIVAL:
                    Order order = (Order) event.getData();
                    futureState.addOrder(order);
                    break;
                case NEW_DAY_BEGIN:
                    futureState.refillDepots();
                    break;
                default:
                    break;
            }
        }

        logger.debug("Eventos futuros aplicados al estado proyectado");
    }

    private Map<String, VehiclePlan> generateNewPlans(SimulationState futureState) {
        logger.debug("Generando nuevos planes para estado futuro en tiempo: {}", futureState.getCurrentTime());
        logger.debug("Estado futuro contiene: {} vehículos, {} órdenes",
                futureState.getVehicles().size(),
                futureState.getOrders().size());

        Map<String, VehiclePlan> newPlans = new HashMap<>();

        try {
            Solution solution = MetaheuristicSolver.solve(futureState);

            if (solution == null) {
                logger.warn("El solver no pudo encontrar una solución válida");
                return newPlans;
            }

            logger.info("Solución generada con costo total: {}", solution.getCost().totalCost());

            // Crear planes de vehículo a partir de las rutas en la solución
            for (Map.Entry<String, Route> entry : solution.getRoutes().entrySet()) {
                String vehicleId = entry.getKey();
                Route route = entry.getValue();
                Vehicle vehicle = futureState.getVehicleById(vehicleId);

                if (vehicle == null) {
                    logger.error("Vehículo no encontrado para ID: {}", vehicleId);
                    continue;
                }

                try {
                    VehiclePlan plan = VehiclePlanCreator.createPlanFromRoute(route, futureState);
                    if (plan != null) {
                        newPlans.put(vehicleId, plan);
                    }
                } catch (Exception e) {
                    logger.error("Error al crear plan para vehículo {}: {}", vehicleId, e.getMessage(), e);
                }
            }

            logger.info("Generación de planes completada con éxito: {} planes creados", newPlans.size());
        } catch (Exception e) {
            logger.error("Error durante la generación de planes: {}", e.getMessage(), e);
        }

        return newPlans;
    }

    private void checkApplyFuturePlans(LocalDateTime nextTickTime) {
        if (targetPlanningTime != null && !futurePlans.isEmpty()) {
            if (nextTickTime.isEqual(targetPlanningTime) || nextTickTime.isAfter(targetPlanningTime)) {
                logger.info("Aplicando planes futuros generados para tiempo: {}", targetPlanningTime);

                // Reemplazar los planes actuales con los futuros
                synchronized (this) {
                    int prevPlansCount = state.getCurrentVehiclePlans().size();
                    state.getCurrentVehiclePlans().clear();
                    state.getCurrentVehiclePlans().putAll(futurePlans);
                    logger.debug("Planes anteriores: {}, nuevos planes aplicados: {}",
                            prevPlansCount, futurePlans.size());
                    futurePlans.clear();
                    targetPlanningTime = null;
                }
            } else {
                logger.debug("Tiempo actual ({}) aún no alcanza el objetivo para planes ({}) - Esperando...",
                        nextTickTime, targetPlanningTime);
            }
        }
    }

    private void processEvent(Event event) {
        logger.info("Processing event: {}", event);
        switch (event.getType()) {
            case ORDER_ARRIVAL:
                Order order = (Order) event.getData();
                state.addOrder(order);
                break;
            case MAINTENANCE_START:
                Maintenance maintenance = (Maintenance) event.getData();
                Vehicle vehicleForMaintenance = state.getVehicleById(maintenance.getVehicle().getId());
                if (vehicleForMaintenance != null) {
                    // Remover plan actual si existe
                    state.removeVehiclePlan(vehicleForMaintenance.getId());

                    // Crear un plan para llevar el vehículo a la planta principal
                    VehiclePlan planToMainDepot = VehiclePlanCreator.createPlanToMainDepot(vehicleForMaintenance,
                            state);
                    if (planToMainDepot != null) {
                        state.addVehiclePlan(vehicleForMaintenance.getId(), planToMainDepot);
                        logger.info("Vehículo {} enviado a mantenimiento, plan creado", vehicleForMaintenance.getId());
                    }

                    // Marcar el vehículo en mantenimiento
                    vehicleForMaintenance.setMaintenance();
                    state.addMaintenance(maintenance);
                }

                // Cancelar planificación en progreso y replanificar inmediatamente
                cancelCurrentPlanningAndReplan();
                break;
            case MAINTENANCE_END:
                replanFlag = true;
                logger.debug("Fin de mantenimiento detectado, replanificación solicitada");
                break;
            case VEHICLE_BREAKDOWN:
                Incident incident = (Incident) event.getData();
                Vehicle vehicleWithIncident = incident.getVehicle();
                if (vehicleWithIncident != null) {
                    // Remove the current plan if it exists
                    state.removeVehiclePlan(vehicleWithIncident.getId());
                    
                    // Create a plan for the incident handling
                    VehiclePlan incidentPlan = VehiclePlanCreator.createPlanForIncident(
                        vehicleWithIncident, 
                        incident, 
                        state
                    );
                    
                    if (incidentPlan != null) {
                        state.addVehiclePlan(vehicleWithIncident.getId(), incidentPlan);
                        logger.info("Incident plan created for vehicle {}, type: {}", 
                                vehicleWithIncident.getId(), incident.getType());
                    }
                    
                    // Mark the vehicle with incident and add to the state
                    vehicleWithIncident.setIncident();
                    state.addIncident(incident);
                }
                
                // Cancel current planning and replan immediately
                cancelCurrentPlanningAndReplan();
                break;
            case NEW_DAY_BEGIN:
                state.refillDepots();
                break;
            default:
                break;
        }
    }

    private void cancelCurrentPlanningAndReplan() {
        // Si hay una planificación en curso, cancelarla
        if (planningInProgress && currentPlanningTask != null && !currentPlanningTask.isDone()) {
            logger.info("Cancelando planificación en curso debido a evento crítico");
            currentPlanningTask.cancel(true);
            planningInProgress = false;
        }

        // Iniciar nueva planificación inmediatamente
        logger.info("Iniciando replanificación inmediata debido a evento crítico");
        startAsyncReplanification();
        replanFlag = false;
        ticksToReplan = FUTURE_PROJECTION_MINUTES;
    }

    public void shutdown() {
        logger.info("Apagando Orchestrator y su planificador");
        if (currentPlanningTask != null && !currentPlanningTask.isDone()) {
            currentPlanningTask.cancel(true);
            logger.debug("Tarea de planificación en curso cancelada");
        }
        plannerExecutor.shutdown();
    }
}
