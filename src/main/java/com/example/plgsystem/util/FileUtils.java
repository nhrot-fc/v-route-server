package com.example.plgsystem.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;

public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    private static final Pattern ORDER_PATTERN = Pattern.compile("(\\d+d\\d+h\\d+m):(.+)");
    private static final Pattern BLOCKAGE_PATTERN = Pattern.compile("(\\d+d\\d+h\\d+m)-(\\d+d\\d+h\\d+m):(.+)");
    private static final Map<String, Path> TEMP_FILE_CACHE = new HashMap<>();

    // File cache management methods
    private static String generateCacheKey(String simulationId, int year, int month, String type) {
        return String.format("%s-%s-%d-%02d", simulationId, type, year, month);
    }

    public static Path getValidatedFile(String simulationId, int year, int month, String type) {
        String cacheKey = generateCacheKey(simulationId, year, month, type);
        return TEMP_FILE_CACHE.get(cacheKey);
    }

    public static void cleanupTempFiles() {
        for (Path path : TEMP_FILE_CACHE.values()) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                logger.error("Error al limpiar archivos temporales: {}", path, e);
            }
        }
        TEMP_FILE_CACHE.clear();
    }

    public static void cleanupTempFilesForSimulation(String simulationId) {
        List<String> keysToRemove = new ArrayList<>();

        for (Map.Entry<String, Path> entry : TEMP_FILE_CACHE.entrySet()) {
            if (entry.getKey().startsWith(simulationId + "-")) {
                try {
                    Files.deleteIfExists(entry.getValue());
                    keysToRemove.add(entry.getKey());
                } catch (IOException e) {
                    logger.error("Error al limpiar archivos temporales para simulación: {}", simulationId, e);
                }
            }
        }

        for (String key : keysToRemove) {
            TEMP_FILE_CACHE.remove(key);
        }
    }

    // File validation methods
    public static Path validateOrdersFile(MultipartFile file, LocalDate startDate, String simulationId)
            throws IOException {
        int totalLines = 0;
        int validLines = 0;
        int invalidLines = 0;

        String cacheKey = generateCacheKey(simulationId, startDate.getYear(), startDate.getMonthValue(),
                "orders");
        if (TEMP_FILE_CACHE.containsKey(cacheKey)) {
            return TEMP_FILE_CACHE.get(cacheKey);
        }

        Path tempPath = Files.createTempFile(cacheKey, ".txt");
        Files.copy(file.getInputStream(), tempPath, StandardCopyOption.REPLACE_EXISTING);

        try (BufferedReader reader = Files.newBufferedReader(tempPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                try {
                    Matcher matcher = ORDER_PATTERN.matcher(line);

                    if (!matcher.matches()) {
                        invalidLines++;
                        continue;
                    }

                    String timeStr = matcher.group(1);
                    String[] data = matcher.group(2).split(",");

                    if (data.length < 5) {
                        invalidLines++;
                        continue;
                    }

                    DateTimeParser.parseDateTime(startDate, timeStr);
                    Integer.parseInt(data[0]);
                    Integer.parseInt(data[1]);

                    if (!data[3].matches("\\d+m3")) {
                        invalidLines++;
                        continue;
                    }

                    if (!data[4].matches("\\d+h")) {
                        invalidLines++;
                        continue;
                    }

                    validLines++;
                } catch (Exception e) {
                    invalidLines++;
                }
            }
        }

        if (invalidLines > 0) {
            Files.deleteIfExists(tempPath);
            throw new IllegalArgumentException(String.format(
                    "Archivo de órdenes contiene errores: %d líneas totales, %d válidas, %d inválidas",
                    totalLines, validLines, invalidLines));
        }
        TEMP_FILE_CACHE.put(cacheKey, tempPath);
        return tempPath;
    }

    public static Path validateBlockagesFile(MultipartFile file, LocalDate startDate, String simulationId)
            throws IOException {
        int totalLines = 0;
        int validLines = 0;
        int invalidLines = 0;

        String cacheKey = generateCacheKey(simulationId, startDate.getYear(), startDate.getMonthValue(),
                "blockages");
        if (TEMP_FILE_CACHE.containsKey(cacheKey)) {
            return TEMP_FILE_CACHE.get(cacheKey);
        }
        Path tempPath = Files.createTempFile(cacheKey, ".txt");
        Files.copy(file.getInputStream(), tempPath, StandardCopyOption.REPLACE_EXISTING);

        try (BufferedReader reader = Files.newBufferedReader(tempPath)) {
            String line;

            while ((line = reader.readLine()) != null) {
                totalLines++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                try {
                    Matcher matcher = BLOCKAGE_PATTERN.matcher(line);

                    if (!matcher.matches()) {
                        invalidLines++;
                        continue;
                    }

                    String startTimeStr = matcher.group(1);
                    String endTimeStr = matcher.group(2);
                    String coordinatesStr = matcher.group(3);

                    DateTimeParser.parseDateTime(startDate, startTimeStr);
                    DateTimeParser.parseDateTime(startDate, endTimeStr);

                    String[] coordinates = coordinatesStr.split(",");
                    if (coordinates.length < 4 || coordinates.length % 2 != 0) {
                        invalidLines++;
                        continue;
                    }

                    for (String coordinate : coordinates) {
                        Integer.parseInt(coordinate);
                    }

                    validLines++;
                } catch (Exception e) {
                    invalidLines++;
                }
            }
        }

        if (invalidLines > 0) {
            Files.deleteIfExists(tempPath);
            throw new IllegalArgumentException(String.format(
                    "Archivo de bloqueos contiene errores: %d líneas totales, %d válidas, %d inválidas",
                    totalLines, validLines, invalidLines));
        }
        TEMP_FILE_CACHE.put(cacheKey, tempPath);
        return tempPath;
    }

    // Data loading methods
    public static List<Order> loadOrdersForDate(Path filePath, LocalDate startDate, LocalDate endDate)
            throws IOException {
        List<Order> ordersForDate = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                Matcher matcher = ORDER_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String timeStr = matcher.group(1);
                    String[] data = matcher.group(2).split(",");

                    LocalDateTime arrivalTime = DateTimeParser.parseDateTime(startDate, timeStr);
                    if (arrivalTime.toLocalDate().equals(endDate)) {
                        break;
                    }
                    int glpRequestM3 = Integer.parseInt(data[3].replace("m3", ""));
                    int expirationHours = Integer.parseInt(data[4].replace("h", "")) + 4;
                    Position position = new Position(Integer.parseInt(data[0]), Integer.parseInt(data[1]));
                    String id = data[2] + arrivalTime.toString();
                    Order order = new Order(id, arrivalTime, arrivalTime.plusHours(expirationHours), glpRequestM3,
                            position);
                    ordersForDate.add(order);
                }
            }
        }

        return ordersForDate;
    }

    public static List<Blockage> loadBlockagesForDate(Path filePath, LocalDate startDate, LocalDate endDate)
            throws IOException {
        List<Blockage> blockagesForDate = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                Matcher matcher = BLOCKAGE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String startTimeStr = matcher.group(1);
                    String endTimeStr = matcher.group(2);
                    String coordinatesStr = matcher.group(3);

                    LocalDateTime startTime = DateTimeParser.parseDateTime(startDate, startTimeStr);
                    LocalDateTime endTime = DateTimeParser.parseDateTime(startDate, endTimeStr);

                    if (startTime.toLocalDate().equals(endDate)) {
                        break;
                    }
                    String[] coordinates = coordinatesStr.split(",");
                    List<Position> blockagePoints = new ArrayList<>();
                    for (int i = 0; i < coordinates.length; i += 2) {
                        int x = Integer.parseInt(coordinates[i]);
                        int y = Integer.parseInt(coordinates[i + 1]);
                        blockagePoints.add(new Position(x, y));
                    }

                    Blockage blockage = new Blockage(startTime, endTime, blockagePoints);
                    blockagesForDate.add(blockage);
                }
            }
        }

        return blockagesForDate;
    }
}