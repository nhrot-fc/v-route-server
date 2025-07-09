package com.example.plgsystem.orchest;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.repository.BlockageRepository;
import com.example.plgsystem.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de DataLoader que carga datos desde la base de datos.
 */
public class DatabaseDataLoader implements DataLoader {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseDataLoader.class);

    private final OrderRepository orderRepository;
    private final BlockageRepository blockageRepository;

    /**
     * Constructor para DatabaseDataLoader
     * 
     * @param orderRepository    Repositorio para órdenes
     * @param blockageRepository Repositorio para bloqueos
     */
    public DatabaseDataLoader(
            OrderRepository orderRepository,
            BlockageRepository blockageRepository) {
        this.orderRepository = orderRepository;
        this.blockageRepository = blockageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> loadOrdersForDate(LocalDate date) {
        logger.info("Cargando órdenes desde la base de datos para la fecha: {}", date);

        // Establecer el rango de tiempo para el día
        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);

        // Buscar órdenes que lleguen en la fecha especificada
        List<Order> orders = orderRepository.findByArrivalTimeBetween(startOfDay, endOfDay);
        List<Event> events = new ArrayList<>();

        if (!orders.isEmpty()) {
            logger.info("Encontradas {} órdenes para la fecha {}", orders.size(), date);

            // Crear eventos para cada orden
            for (Order order : orders) {
                Event event = new Event(EventType.ORDER_ARRIVAL, order.getArrivalTime(), order.getId(), order);
                events.add(event);
            }

            logger.info("Creados {} eventos de órdenes para la fecha {}", orders.size(), date);
        } else {
            logger.info("No hay órdenes en la base de datos para la fecha: {}", date);
        }

        return events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> loadBlockagesForDate(LocalDate date) {
        logger.info("Cargando bloqueos desde la base de datos para la fecha: {}", date);

        // Establecer el rango de tiempo para el día
        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);

        // Buscar bloqueos activos en la fecha especificada
        List<Blockage> blockages = blockageRepository.findActiveBlockagesForPeriod(startOfDay, endOfDay);
        List<Event> events = new ArrayList<>();

        if (!blockages.isEmpty()) {
            logger.info("Encontrados {} bloqueos para la fecha {}", blockages.size(), date);

            // Crear eventos para cada bloqueo
            for (Blockage blockage : blockages) {
                // Evento de inicio del bloqueo
                Event startEvent = new Event(
                        EventType.BLOCKAGE_START,
                        blockage.getStartTime(),
                        blockage.getId().toString(),
                        blockage);

                // Evento de fin del bloqueo
                Event endEvent = new Event(
                        EventType.BLOCKAGE_END,
                        blockage.getEndTime(),
                        blockage.getId().toString(),
                        null);

                events.add(startEvent);
                events.add(endEvent);
            }

            logger.info("Creados {} eventos de bloqueos para la fecha {}", blockages.size() * 2, date);
        } else {
            logger.info("No hay bloqueos en la base de datos para la fecha: {}", date);
        }

        return events;
    }
}