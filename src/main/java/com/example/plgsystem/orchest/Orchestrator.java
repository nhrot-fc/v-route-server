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
        this.dataLoader = dataLoader;
        this.state = state;
        this.eventQueue = new PriorityQueue<>();

        this.ticksToCheckEvents = 10;
        this.ticksToReplan = 60;
        this.replanFlag = false;

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
        if (ticksToCheckEvents == 0) {
            checkAndLoadNewEvents();
            ticksToCheckEvents = 10;
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
        if (ticksToReplan % 10 == 0) {
            logger.debug("Ticks hasta próxima replanificación: {}", ticksToReplan);
        }
    }

    private void checkAndLoadNewEvents() {
        logger.info("Checking for new events from data loader");
        Set<String> currentOrders = new HashSet<>();
        Set<String> currentBlockages = new HashSet<>();

        for (Event event : eventQueue) {
            if (event.getType() == EventType.ORDER_ARRIVAL) {
                currentOrders.add(event.getEntityId());
            }
            if (event.getType() == EventType.BLOCKAGE_START) {
                Blockage blockage = (Blockage) event.getData();
                String blockageCustomId = String.format("%03d-%03d-%03d-%03d-%s-%s",
                        (int) blockage.getLines().get(0).getX(), (int) blockage.getLines().get(0).getY(),
                        (int) blockage.getLines().get(1).getX(), (int) blockage.getLines().get(1).getY(),
                        blockage.getStartTime(), blockage.getEndTime());
                currentBlockages.add(blockageCustomId);
            }
        }

        for (Order order : state.getOrders()) {
            currentOrders.add(order.getId());
        }
        for (Blockage blockage : state.getBlockages()) {
            String blockageCustomId = String.format("%03d-%03d-%03d-%03d-%s-%s",
                    (int) blockage.getLines().get(0).getX(), (int) blockage.getLines().get(0).getY(),
                    (int) blockage.getLines().get(1).getX(), (int) blockage.getLines().get(1).getY(),
                    blockage.getStartTime(), blockage.getEndTime());
            currentBlockages.add(blockageCustomId);
        }

        List<Event> newOrderEvents = dataLoader.loadOrdersForDate(state.getCurrentTime().toLocalDate());
        List<Event> newBlockageEvents = dataLoader.loadBlockagesForDate(state.getCurrentTime().toLocalDate());

        List<Event> filteredEvents = new ArrayList<>();
        for (Event event : newOrderEvents) {
            if (event.getType() == EventType.ORDER_ARRIVAL) {
                Order order = (Order) event.getData();

                if (!currentOrders.contains(order.getId()) && event.getTime().isAfter(state.getCurrentTime())) {
                    filteredEvents.add(event);
                }
            }
        }

        for (Event event : newBlockageEvents) {
            if (event.getType() == EventType.BLOCKAGE_START) {
                Blockage blockage = (Blockage) event.getData();

                String blockageCustomId = String.format("%03d-%03d-%03d-%03d-%s-%s",
                        (int) blockage.getLines().get(0).getX(), (int) blockage.getLines().get(0).getY(),
                        (int) blockage.getLines().get(1).getX(), (int) blockage.getLines().get(1).getY(),
                        blockage.getStartTime(), blockage.getEndTime());

                if (!currentBlockages.contains(blockageCustomId) && event.getTime().isAfter(state.getCurrentTime())) {
                    filteredEvents.add(event);
                    state.addBlockage(blockage);
                }
            }
        }

        addEvents(filteredEvents);
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
            ticksToReplan = 60;
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
        // Creamos un snapshot del estado actual
        SimulationState futureState = state.createSnapshot();

        // Proyectamos el estado 60 minutos en el futuro
        LocalDateTime projectedTime = futureState.getCurrentTime().plusMinutes(60);

        // Procesamos los eventos que ocurrirían dentro de la ventana de proyección
        applyEventsToFutureState(futureState, projectedTime);

        // Avanzamos el tiempo del estado futuro
        futureState.advanceTime(Duration.between(futureState.getCurrentTime(), projectedTime));

        // Guardamos el tiempo objetivo para el que estamos planificando
        targetPlanningTime = projectedTime;

        logger.info("Iniciando replanificación asíncrona para el tiempo: {}", targetPlanningTime);
        planningInProgress = true;

        // Lanzamos la tarea de replanificación en un thread separado
        currentPlanningTask = plannerExecutor.submit(() -> {
            Thread.currentThread().setName("PlannerThread");
            logger.debug("Thread de planificación iniciado para tiempo objetivo: {}", targetPlanningTime);
            try {
                // Aquí iría la lógica de replanificación que usa futureState
                Map<String, VehiclePlan> newPlans = generateNewPlans(futureState);

                // Guardamos los nuevos planes para aplicarlos cuando sea el momento
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
                case BLOCKAGE_START:
                    Blockage blockage = (Blockage) event.getData();
                    futureState.addBlockage(blockage);
                    break;
                case MAINTENANCE_START:
                    // Aplicar inicio de mantenimiento al estado futuro
                    break;
                case MAINTENANCE_END:
                    // Aplicar fin de mantenimiento al estado futuro
                    break;
                case VEHICLE_BREAKDOWN:
                    // No proyectamos incidentes futuros ya que son imprevisibles
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
            // Usar el MetaheuristicSolver para generar una solución optimizada
            logger.info("Iniciando proceso de optimización con MetaheuristicSolver");
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
                        logger.debug("Plan generado para vehículo {}: {} acciones",
                                vehicleId, plan.getActions().size());
                    } else {
                        logger.warn("No se pudo crear plan para vehículo: {}", vehicleId);
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
                Vehicle vehicle = state.getVehicleById(incident.getVehicle().getId());
                vehicle.setIncident();
                state.addIncident(incident);
                state.removeVehiclePlan(vehicle.getId());

                // Cancelar planificación en progreso y replanificar inmediatamente
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
        ticksToReplan = 60;
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
