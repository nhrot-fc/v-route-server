package com.example.plgsystem.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.repository.BlockageRepository;
import com.example.plgsystem.repository.OrderRepository;
import com.example.plgsystem.util.DateTimeParser;

/**
 * Servicio para leer y cargar datos desde archivos
 */
@Service
public class FileReaderService {

    private static final Pattern ORDER_PATTERN = Pattern.compile("(\\d+d\\d+h\\d+m):(.+)");
    private static final Pattern BLOCKAGE_PATTERN = Pattern.compile("(\\d+d\\d+h\\d+m)-(\\d+d\\d+h\\d+m):(.+)");

    private final OrderRepository orderRepository;
    private final BlockageRepository blockageRepository;

    public FileReaderService(OrderRepository orderRepository, BlockageRepository blockageRepository) {
        this.orderRepository = orderRepository;
        this.blockageRepository = blockageRepository;
    }

    /**
     * Carga órdenes desde un archivo con formato específico.
     * Ejemplo de formato:
     * 01d00h24m:16,13,c-198,3m3,4h
     * 
     * @param file          Archivo con datos de órdenes
     * @param referenceDate Fecha de referencia para calcular fechas completas
     * @return Lista de órdenes cargadas
     * @throws IOException Si hay problemas al leer el archivo
     */
    public List<Order> loadOrdersFromFile(MultipartFile file, LocalDate referenceDate) throws IOException {
        List<Order> orders = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Ignorar líneas vacías y comentarios
                }

                try {
                    Matcher matcher = ORDER_PATTERN.matcher(line);

                    if (!matcher.matches()) {
                        System.err.println("Formato de orden inválido en línea " + lineNumber + ": " + line);
                        continue;
                    }

                    // Extraer tiempo y datos
                    String timeStr = matcher.group(1);
                    String[] data = matcher.group(2).split(",");

                    if (data.length < 5) {
                        System.err.println("Datos de orden insuficientes en línea " + lineNumber +
                                ": se esperan al menos 5 valores (x,y,cliente,volumen,horas)");
                        continue;
                    }

                    // Parsear datos
                    LocalDateTime arrivalTime = DateTimeParser.parseDateTime(referenceDate, timeStr);
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Position position = new Position(x, y);

                    String clientCode = data[2];

                    // Extraer el valor numérico del volumen (3m3 -> 3)
                    String volumeStr = data[3].split("m")[0];
                    int glpRequestM3 = Integer.parseInt(volumeStr);

                    // Extraer el valor numérico de las horas límite (4h -> 4)
                    String hoursStr = data[4].replaceAll("[^0-9]", "");
                    long limitTimeHours = Long.parseLong(hoursStr);

                    LocalDateTime deadlineTime = arrivalTime.plusHours(limitTimeHours);

                    // Generar ID único para la orden
                    String orderId = clientCode + "-" + arrivalTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

                    // Verificar duplicados
                    if (orders.stream().anyMatch(o -> o.getId().equals(orderId))) {
                        System.err.println("Omitiendo orden duplicada con ID: " + orderId);
                        continue;
                    }

                    // Crear y añadir la orden
                    Order order = new Order(orderId, arrivalTime, deadlineTime, glpRequestM3, position);
                    orders.add(order);

                } catch (Exception e) {
                    System.err.println("Error al procesar orden en línea " + lineNumber + ": " + e.getMessage());
                }
            }
        }

        return orders;
    }

    /**
     * Carga bloqueos desde un archivo con formato específico.
     * Ejemplo de formato:
     * 01d00h31m-01d21h35m:15,10,30,10,30,18
     * 
     * @param file          Archivo con datos de bloqueos
     * @param referenceDate Fecha de referencia para calcular fechas completas
     * @return Lista de bloqueos cargados
     * @throws IOException Si hay problemas al leer el archivo
     */
    public List<Blockage> loadBlockagesFromFile(MultipartFile file, LocalDate referenceDate) throws IOException {
        List<Blockage> loadedBlockages = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Ignorar líneas vacías y comentarios
                }

                try {
                    Matcher matcher = BLOCKAGE_PATTERN.matcher(line);

                    if (!matcher.matches()) {
                        System.err.println("Formato de bloqueo inválido en línea " + lineNumber + ": " + line);
                        continue;
                    }

                    // Extraer tiempos de inicio y fin
                    String startTimeStr = matcher.group(1);
                    String endTimeStr = matcher.group(2);
                    String coordinatesStr = matcher.group(3);

                    // Parsear fechas usando DateTimeParser
                    LocalDateTime startTime = DateTimeParser.parseDateTime(referenceDate, startTimeStr);
                    LocalDateTime endTime = DateTimeParser.parseDateTime(referenceDate, endTimeStr);

                    // Parsear coordenadas
                    String[] coordinates = coordinatesStr.split(",");
                    if (coordinates.length < 4 || coordinates.length % 2 != 0) {
                        System.err.println("Formato de coordenadas inválido en línea " + lineNumber +
                                ": se esperan al menos 2 puntos (4 valores) y todos los puntos deben ser pares x,y");
                        continue;
                    }

                    List<Position> blockagePoints = new ArrayList<>();
                    for (int i = 0; i < coordinates.length; i += 2) {
                        int x = Integer.parseInt(coordinates[i]);
                        int y = Integer.parseInt(coordinates[i + 1]);
                        blockagePoints.add(new Position(x, y));
                    }

                    // Crear y añadir el bloqueo
                    Blockage blockage = new Blockage(startTime, endTime, blockagePoints);
                    loadedBlockages.add(blockage);

                } catch (Exception e) {
                    System.err.println("Error al procesar bloqueo en línea " + lineNumber + ": " + e.getMessage());
                }
            }
        }

        return loadedBlockages;
    }

    /**
     * Carga y guarda órdenes desde un archivo
     * 
     * @param file          Archivo de órdenes
     * @param referenceDate Fecha de referencia
     * @return Lista de órdenes cargadas y guardadas
     * @throws IOException Si hay problemas al leer el archivo
     */
    @Transactional
    public List<Order> loadAndSaveOrders(MultipartFile file, LocalDate referenceDate) throws IOException {
        List<Order> orders = loadOrdersFromFile(file, referenceDate);
        return orderRepository.saveAll(orders);
    }

    /**
     * Carga y guarda bloqueos desde un archivo
     * 
     * @param file          Archivo de bloqueos
     * @param referenceDate Fecha de referencia
     * @return Lista de bloqueos cargados y guardados
     * @throws IOException Si hay problemas al leer el archivo
     */
    @Transactional
    public List<Blockage> loadAndSaveBlockages(MultipartFile file, LocalDate referenceDate) throws IOException {
        List<Blockage> blockages = loadBlockagesFromFile(file, referenceDate);
        return blockageRepository.saveAll(blockages);
    }
}
