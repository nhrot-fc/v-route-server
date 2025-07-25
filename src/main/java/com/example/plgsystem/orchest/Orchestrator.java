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

@Getter
public class Orchestrator {
    private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);
    private static final int TICKS_TO_CHECK_EVENTS = 10;
    private static final int DAILY_OPS_PROJECTION_MINUTES = 2;
    private static final int NORMAL_PROJECTION_MINUTES = 90;

    private final boolean isDailyOperation;
    private final SimulationState state;
    private final PriorityQueue<Event> eventQueue;
    private final DataLoader dataLoader;

    // Sets to track already processed file-based events
    private final Set<String> processedOrderIds = new HashSet<>();
    private final Set<String> processedBlockageIds = new HashSet<>();

    private int ticksToCheckEvents;
    private int ticksToReplan;
    private boolean replanFlag;
    private LocalDateTime lastReplanTime;

    // Nuevos atributos para replanificación asíncrona
    private final ExecutorService plannerExecutor;
    private Future<?> currentPlanningTask;
    private LocalDateTime targetPlanningTime;
    private Map<String, VehiclePlan> futurePlans;
    private boolean planningInProgress;

    public Orchestrator(SimulationState state, DataLoader dataLoader, boolean isDailyOperation) {
        this.isDailyOperation = isDailyOperation;
        this.dataLoader = dataLoader;
        this.state = state;
        this.eventQueue = new PriorityQueue<>(Event::compareTo);
        eventQueue.add(new Event(EventType.NEW_DAY,
                state.getCurrentTime().plusDays(1).withHour(0).withMinute(0), null, null));

        if (isDailyOperation) {
            this.lastReplanTime = state.getCurrentTime();
        }
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
        // Daily operations don't need tick-based management for most operations
        if (isDailyOperation) {
            checkAndLoadNewEvents();
        } else if (ticksToCheckEvents <= 0) {
            checkAndLoadNewEvents();
            ticksToCheckEvents = TICKS_TO_CHECK_EVENTS;
        }

        LocalDateTime nextTickTime = isDailyOperation ? LocalDateTime.now() : state.getCurrentTime().plusMinutes(1);
        pollEvents(nextTickTime);
        checkReplanification();
        checkApplyFuturePlans(nextTickTime);
        state.advanceTime(Duration.between(state.getCurrentTime(), nextTickTime));

        if (!isDailyOperation) {
            ticksToCheckEvents--;
            ticksToReplan--;
        }
    }

    private void checkAndLoadNewEvents() {
        // Track existing orders and blockages to avoid duplicates
        Set<String> currentOrderIds = new HashSet<>();
        Set<String> currentBlockageIds = new HashSet<>();

        // Add orders and blockages from the event queue to our tracking sets
        for (Event event : eventQueue) {
            if (event.getType() == EventType.ORDER) {
                currentOrderIds.add(event.getEntityId());
            } else if (event.getType() == EventType.BLOCKAGE) {
                Blockage blockage = (Blockage) event.getData();
                String blockageId = createBlockageIdentifier(blockage);
                currentBlockageIds.add(blockageId);
            }
        }

        // Add orders and blockages from the current state to our tracking sets
        for (Order order : state.getOrders()) {
            currentOrderIds.add(order.getId());
            processedOrderIds.add(order.getId()); // Track as processed for file-based loading
        }
        for (Blockage blockage : state.getBlockages()) {
            String blockageId = createBlockageIdentifier(blockage);
            currentBlockageIds.add(blockageId);
            processedBlockageIds.add(blockageId); // Track as processed for file-based loading
        }

        // Load potential new events for the current date
        List<Event> newOrderEvents = dataLoader.loadOrdersForDate(state.getCurrentTime().toLocalDate());
        List<Event> newBlockageEvents = dataLoader.loadBlockagesForDate(state.getCurrentTime().toLocalDate());

        LocalDateTime currentTime = state.getCurrentTime();
        List<Event> eventsToAdd = new ArrayList<>();
        int newEventsCount = 0;

        // Process order events
        for (Event event : newOrderEvents) {
            if (event.getType() == EventType.ORDER) {
                Order order = (Order) event.getData();

                // Only add if: 1) Not a duplicate, 2) Not previously processed, 3) Deadline is
                // in the future, 4) Has remaining GLP
                if (!currentOrderIds.contains(order.getId()) &&
                        !processedOrderIds.contains(order.getId()) &&
                        order.getDeadlineTime().isAfter(currentTime) &&
                        order.getRemainingGlpM3() > 0) {

                    eventsToAdd.add(event);
                    currentOrderIds.add(order.getId());
                    processedOrderIds.add(order.getId()); // Mark as processed
                    newEventsCount++;
                    logger.debug("New order event added: {} (deadline: {})",
                            order.getId(), order.getDeadlineTime());
                }
            }
        }

        // Process blockage events
        for (Event event : newBlockageEvents) {
            if (event.getType() == EventType.BLOCKAGE) {
                Blockage blockage = (Blockage) event.getData();
                String blockageId = createBlockageIdentifier(blockage);

                // Only add if: 1) Not a duplicate, 2) Not previously processed, 3) End time is
                // in the future
                if (!currentBlockageIds.contains(blockageId) &&
                        !processedBlockageIds.contains(blockageId) &&
                        blockage.getEndTime().isAfter(currentTime)) {

                    eventsToAdd.add(event);
                    currentBlockageIds.add(blockageId);
                    processedBlockageIds.add(blockageId); // Mark as processed

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
        boolean shouldReplan = false;

        if (isDailyOperation) {
            // For daily operations, check if 2 minutes have passed since last replan
            LocalDateTime now = LocalDateTime.now();
            Duration timeSinceLastReplan = Duration.between(lastReplanTime, now);
            if (timeSinceLastReplan.toMinutes() >= DAILY_OPS_PROJECTION_MINUTES || replanFlag) {
                shouldReplan = true;
                lastReplanTime = now; // Update last replan time
                logger.debug("Daily operations: Replanificando después de {} minutos",
                        timeSinceLastReplan.toMinutes());
            }
        } else if (ticksToReplan <= 0 || replanFlag) {
            shouldReplan = true;
            ticksToReplan = NORMAL_PROJECTION_MINUTES;
        }

        // Si es tiempo de replanificar y no hay planificación en curso
        if (shouldReplan && !planningInProgress) {
            logger.debug("Iniciando proceso de replanificación: ticksToReplan={}, replanFlag={}",
                    ticksToReplan, replanFlag);
            startAsyncReplanification();
            replanFlag = false;
        }

        // Verificar vehículos sin plan que estén fuera de planta y enviarlos a planta
        for (Vehicle vehicle : state.getVehicles()) {
            if (!state.getCurrentVehiclePlans().containsKey(vehicle.getId()) &&
                    vehicle.isAvailable() &&
                    !isAtMainDepot(vehicle)) {

                logger.info("Vehículo {} sin plan y fuera de planta, creando plan para retorno a base",
                        vehicle.getId());

                // Create return-to-base plan that respects any current action the vehicle may
                // be performing
                VehiclePlan planToMainDepot = VehiclePlanCreator.createPlanToMainDepot(vehicle, state);

                if (planToMainDepot != null) {
                    state.addVehiclePlan(vehicle.getId(), planToMainDepot);
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

        // Use appropriate projection time based on simulation type
        int projectionMinutes = isDailyOperation ? DAILY_OPS_PROJECTION_MINUTES : NORMAL_PROJECTION_MINUTES;
        LocalDateTime projectedTime = futureState.getCurrentTime().plusMinutes(projectionMinutes);

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
                case ORDER:
                    Order order = (Order) event.getData();
                    futureState.addOrder(order);
                    break;
                case NEW_DAY:
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

                // Reemplazar los planes actuales con los futuros, preservando acciones en curso
                synchronized (this) {
                    int prevPlansCount = state.getCurrentVehiclePlans().size();

                    for (Map.Entry<String, VehiclePlan> entry : futurePlans.entrySet()) {
                        String vehicleId = entry.getKey();
                        Vehicle vehicle = state.getVehicleById(vehicleId);

                        if (vehicle != null) {
                            state.getCurrentVehiclePlans().put(vehicleId, entry.getValue());
                        }
                    }
                    logger.info("Planes aplicados: {} (antes: {})",
                            state.getCurrentVehiclePlans().size(), prevPlansCount);
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
            case ORDER:
                Order order = (Order) event.getData();
                state.addOrder(order);
                break;
            case BREAKDOWN:
                Incident incident = (Incident) event.getData();
                Vehicle vehicleWithIncident = incident.getVehicle();
                if (vehicleWithIncident != null) {
                    // Mark the vehicle with incident and add to the state
                    vehicleWithIncident.setIncident();
                    state.addIncident(incident);
                    VehiclePlan incidentPlan = VehiclePlanCreator.createPlanForIncident(
                            vehicleWithIncident,
                            incident,
                            state);

                    if (incidentPlan != null) {
                        state.addVehiclePlan(vehicleWithIncident.getId(), incidentPlan);
                        logger.debug("Plan de incidente creado para vehículo {}: {}", vehicleWithIncident.getId(),
                                incidentPlan);
                    }
                }

                // Cancel current planning and replan immediately
                cancelCurrentPlanningAndReplan();
                break;
            case NEW_DAY:
                state.refillDepots();
                processedOrderIds.clear();
                processedBlockageIds.clear();
                eventQueue
                        .add(new Event(EventType.NEW_DAY, state.getCurrentTime().plusDays(1).withHour(0).withMinute(0),
                                null, null));
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

        // Reset ticksToReplan for normal simulations
        if (!isDailyOperation) {
            ticksToReplan = NORMAL_PROJECTION_MINUTES;
        }

        // For daily operations, lastReplanTime is already updated in
        // startAsyncReplanification
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
