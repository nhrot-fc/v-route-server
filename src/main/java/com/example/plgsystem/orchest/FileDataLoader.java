package com.example.plgsystem.orchest;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de DataLoader que carga datos desde archivos temporales.
 */
public class FileDataLoader implements DataLoader {
    private static final Logger logger = LoggerFactory.getLogger(FileDataLoader.class);

    private final Map<String, Path> ordersFilePaths = new HashMap<>();
    private final Map<String, Path> blockagesFilePaths = new HashMap<>();

    public void registerOrdersFile(int year, int month, Path path) {
        String yearMonthKey = year + "-" + String.format("%02d", month);
        ordersFilePaths.put(yearMonthKey, path);
        logger.debug("Registrado archivo de órdenes para {}: {}", yearMonthKey, path);
    }

    public void registerBlockagesFile(int year, int month, Path path) {
        String yearMonthKey = year + "-" + String.format("%02d", month);
        blockagesFilePaths.put(yearMonthKey, path);
        logger.debug("Registrado archivo de bloqueos para {}: {}", yearMonthKey, path);
    }

    @Override
    public List<Event> loadOrdersForDate(LocalDate date) {
        String yearMonthKey = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
        Path ordersPath = ordersFilePaths.get(yearMonthKey);

        if (ordersPath == null) {
            logger.warn("No hay archivo de órdenes disponible para la fecha: {}", yearMonthKey);
            return new ArrayList<>();
        }

        List<Event> events = new ArrayList<>();

        try {
            List<Order> orders = FileUtils.loadOrdersForDate(ordersPath, date, date.plusDays(1));

            if (!orders.isEmpty()) {
                logger.info("Cargando {} órdenes para la fecha {}", orders.size(), date);

                // Crear eventos para cada orden
                for (Order order : orders) {
                    Event event = new Event(EventType.ORDER, order.getArrivalTime(), order.getId(), order);
                    events.add(event);
                }

                logger.info("Creados {} eventos de órdenes para la fecha {}", orders.size(), date);
            } else {
                logger.info("No hay órdenes para la fecha: {}", date);
            }
        } catch (IOException e) {
            logger.error("Error al cargar órdenes para la fecha {}: {}", date, e.getMessage());
        }

        return events;
    }

    @Override
    public List<Event> loadBlockagesForDate(LocalDate date) {
        String yearMonthKey = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
        Path blockagesPath = blockagesFilePaths.get(yearMonthKey);

        if (blockagesPath == null) {
            logger.warn("No hay archivo de bloqueos disponible para la fecha: {}", yearMonthKey);
            return new ArrayList<>();
        }

        List<Event> events = new ArrayList<>();

        try {
            List<Blockage> blockages = FileUtils.loadBlockagesForDate(blockagesPath, date, date.plusDays(2));

            if (!blockages.isEmpty()) {
                logger.info("Cargando {} bloqueos para la fecha {}", blockages.size(), date);

                // Crear eventos para cada bloqueo
                for (Blockage blockage : blockages) {
                    // Evento de inicio del bloqueo
                    Event startEvent = new Event(
                            EventType.BLOCKAGE,
                            blockage.getStartTime(),
                            blockage.getId().toString(),
                            blockage);
                    events.add(startEvent);
                }

                logger.info("Creados {} eventos de bloqueos para la fecha {}", blockages.size() * 2, date);
            }
        } catch (IOException e) {
            logger.error("Error al cargar bloqueos para la fecha {}: {}", date, e.getMessage());
        }

        return events;
    }

    public void cleanup(String simulationId) {
        logger.info("Limpiando recursos de FileDataLoader para simulación: {}", simulationId);
        FileUtils.cleanupTempFilesForSimulation(simulationId);
    }
}