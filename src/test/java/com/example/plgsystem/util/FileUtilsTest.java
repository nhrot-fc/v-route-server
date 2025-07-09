package com.example.plgsystem.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Order;

class FileUtilsTest {

    private final LocalDate referenceDate = LocalDate.of(2025, 1, 1);
    private final String simulationId = "test-simulation-id";

    @AfterEach
    void tearDown() {
        FileUtils.cleanupTempFiles();
    }

    @Test
    void testValidateOrdersFile() throws IOException {
        // Crear un archivo de órdenes válido
        String content = "01d00h24m:16,13,c-198,3m3,4h\n"
                + "01d00h48m:5,18,c-12,9m3,17h";

        MultipartFile file = new MockMultipartFile("orders.txt", "orders.txt", "text/plain",
                content.getBytes());

        Path result = FileUtils.validateOrdersFile(file, referenceDate, simulationId);

        assertNotNull(result);
        assertTrue(Files.exists(result));
        assertTrue(result.toString().contains(simulationId));
        assertTrue(result.toString().contains("orders"));
        assertTrue(result.toString().contains("txt"));
    }

    @Test
    void testValidateOrdersFileWithInvalidContent() {
        // Crear un archivo de órdenes inválido
        String content = "invalid_format:16,13,c-198,3m3,4h\n"
                + "01d00h48m:5,18,c-12,invalid,17h";

        MultipartFile file = new MockMultipartFile("orders.txt", "orders.txt", "text/plain",
                content.getBytes());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            FileUtils.validateOrdersFile(file, referenceDate, simulationId);
        });

        assertTrue(exception.getMessage().contains("errores"));
    }

    @Test
    void testValidateBlockagesFile() throws IOException {
        // Crear un archivo de bloqueos válido
        String content = "01d00h31m-01d21h35m:15,10,30,10,30,18\n"
                + "01d01h13m-01d20h38m:08,03,08,23,20,23";

        MultipartFile file = new MockMultipartFile("blockages.txt", "blockages.txt", "text/plain",
                content.getBytes());

        Path result = FileUtils.validateBlockagesFile(file, referenceDate, simulationId);

        assertNotNull(result);
        assertTrue(Files.exists(result));
        assertTrue(result.toString().contains(simulationId));
        assertTrue(result.toString().contains("blockages"));
        assertTrue(result.toString().contains("txt"));
    }

    @Test
    void testValidateBlockagesFileWithInvalidContent() {
        // Crear un archivo de bloqueos inválido
        String content = "invalid-01d21h35m:15,10,30,10,30,18\n"
                + "01d01h13m-01d20h38m:08,03";

        MultipartFile file = new MockMultipartFile("blockages.txt", "blockages.txt", "text/plain",
                content.getBytes());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            FileUtils.validateBlockagesFile(file, referenceDate, simulationId);
        });

        assertTrue(exception.getMessage().contains("errores"));
    }

    @Test
    void testLoadOrdersForDate() throws IOException {
        // Crear un archivo temporal con datos
        String content = "01d00h24m:16,13,c-198,3m3,4h\n"
                + "01d00h48m:5,18,c-12,9m3,17h\n"
                + "02d01h12m:63,13,c-83,2m3,9h";

        Path tempFile = Files.createTempFile("test_orders_", ".txt");
        Files.write(tempFile, content.getBytes());

        // Probar cargar órdenes para el primer día
        List<Order> day1Orders = FileUtils.loadOrdersForDate(tempFile, referenceDate,
                referenceDate.plusDays(1));
        assertEquals(2, day1Orders.size());

        // Probar cargar órdenes para el segundo día
        LocalDate secondDay = referenceDate.plusDays(2);
        List<Order> day2Orders = FileUtils.loadOrdersForDate(tempFile, referenceDate, secondDay);
        assertEquals(3, day2Orders.size());

        // Limpiar
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testLoadBlockagesForDate() throws IOException {
        // Crear un archivo temporal con datos
        String content = "01d00h31m-01d21h35m:15,10,30,10,30,18\n"
                + "01d01h13m-02d20h38m:08,03,08,23,20,23\n"
                + "02d02h40m-03d22h32m:57,30,57,45,60,35";

        Path tempFile = Files.createTempFile("test_blockages_", ".txt");
        Files.write(tempFile, content.getBytes());

        // Probar cargar bloqueos para el primer día
        List<Blockage> day1Blockages = FileUtils.loadBlockagesForDate(tempFile, referenceDate,
                referenceDate.plusDays(1));
        assertEquals(2, day1Blockages.size());

        // Probar cargar bloqueos para el segundo día
        LocalDate secondDay = referenceDate.plusDays(2);
        List<Blockage> day2Blockages = FileUtils.loadBlockagesForDate(tempFile, referenceDate, secondDay);
        assertEquals(3, day2Blockages.size());

        // Limpiar
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testCleanupTempFilesForSimulation() throws IOException {
        // Crear dos simulaciones con archivos
        String sim1 = "sim1";
        String sim2 = "sim2";

        // Archivos para simulación 1
        MultipartFile file1 = new MockMultipartFile("orders1.txt", "orders1.txt", "text/plain",
                "01d00h24m:16,13,c-198,3m3,4h".getBytes());
        Path path1 = FileUtils.validateOrdersFile(file1, referenceDate, sim1);

        // Archivos para simulación 2
        MultipartFile file2 = new MockMultipartFile("orders2.txt", "orders2.txt", "text/plain",
                "01d00h48m:5,18,c-12,9m3,17h".getBytes());
        Path path2 = FileUtils.validateOrdersFile(file2, referenceDate, sim2);

        // Verificar que existen ambos archivos
        assertTrue(Files.exists(path1));
        assertTrue(Files.exists(path2));

        // Limpiar solo la simulación 1
        FileUtils.cleanupTempFilesForSimulation(sim1);

        // Verificar que el archivo de sim1 ya no existe, pero el de sim2 sí
        assertFalse(Files.exists(path1));
        assertTrue(Files.exists(path2));

        // Limpiar
        FileUtils.cleanupTempFiles();
    }
}