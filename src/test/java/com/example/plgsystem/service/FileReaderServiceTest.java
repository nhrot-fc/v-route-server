package com.example.plgsystem.service;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.repository.BlockageRepository;
import com.example.plgsystem.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

public class FileReaderServiceTest {

    @InjectMocks
    private FileReaderService fileReaderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BlockageRepository blockageRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadOrdersFromFile_withValidData_returnsOrdersList() throws IOException {
        // Arrange
        String csvContent = "01d11h25m:69,49,c-001,3m3,4h\n" +
                "01d12h30m:80,120,c-002,5m3,8h\n";

        MockMultipartFile file = new MockMultipartFile(
                "orders.csv",
                "orders.csv",
                "text/csv",
                csvContent.getBytes());

        LocalDate referenceDate = LocalDate.of(2025, 1, 1);

        // Create the expected orders
        List<Order> expectedOrders = new ArrayList<>();

        Order order1 = new Order(
                "c-001-202501011125",
                LocalDateTime.of(2025, 1, 1, 11, 25),
                LocalDateTime.of(2025, 1, 1, 15, 25),
                3,
                new Position(69, 49));

        Order order2 = new Order(
                "c-002-202501011230",
                LocalDateTime.of(2025, 1, 1, 12, 30),
                LocalDateTime.of(2025, 1, 1, 20, 30),
                5,
                new Position(80, 120));

        expectedOrders.add(order1);
        expectedOrders.add(order2);

        // Act
        List<Order> orders = fileReaderService.loadOrdersFromFile(file, referenceDate);

        // Assert
        assertEquals(2, orders.size());

        // Check first order
        Order firstOrder = orders.get(0);
        System.out.println(firstOrder);
        assertEquals("c-001-202501011125", firstOrder.getId());
        assertEquals(3, firstOrder.getGlpRequestM3());
        assertEquals(3, firstOrder.getRemainingGlpM3());
        assertEquals(69, firstOrder.getPosition().getX());
        assertEquals(49, firstOrder.getPosition().getY());
        assertEquals(LocalDateTime.of(2025, 1, 1, 11, 25), firstOrder.getArrivalTime());
        assertEquals(LocalDateTime.of(2025, 1, 1, 15, 25), firstOrder.getDeadlineTime());

        // Check second order
        Order secondOrder = orders.get(1);
        System.out.println(secondOrder);
        assertEquals("c-002-202501011230", secondOrder.getId());
        assertEquals(5, secondOrder.getGlpRequestM3());
        assertEquals(5, secondOrder.getRemainingGlpM3());
        assertEquals(80, secondOrder.getPosition().getX());
        assertEquals(120, secondOrder.getPosition().getY());
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 30), secondOrder.getArrivalTime());
        assertEquals(LocalDateTime.of(2025, 1, 1, 20, 30), secondOrder.getDeadlineTime());
    }

    @Test
    void loadOrdersFromFile_withInvalidLine_skipsInvalidLines() throws IOException {
        // Arrange
        String csvContent = "01d11h25m:69,49,c-001,3m3,4h\n" +
                "invalid_line\n" +
                "01d12h30m:80,120,c-002,5m3,8h\n";

        MockMultipartFile file = new MockMultipartFile(
                "orders.csv",
                "orders.csv",
                "text/csv",
                csvContent.getBytes());

        LocalDate referenceDate = LocalDate.of(2025, 1, 1);

        // Act
        List<Order> orders = fileReaderService.loadOrdersFromFile(file, referenceDate);

        // Assert
        assertEquals(2, orders.size());
    }

    @Test
    void loadBlockagesFromFile_withValidData_returnsBlockagesList() throws IOException {
        // Arrange
        String csvContent = "01d00h31m-01d21h35m:15,10,30,10,30,18,15,18\n" +
                "02d00h00m-02d23h59m:50,50,60,50,60,60,50,60\n";

        MockMultipartFile file = new MockMultipartFile(
                "blockages.csv",
                "blockages.csv",
                "text/csv",
                csvContent.getBytes());

        LocalDate referenceDate = LocalDate.of(2025, 1, 1);

        // Act
        List<Blockage> blockages = fileReaderService.loadBlockagesFromFile(file, referenceDate);

        // Assert
        assertEquals(2, blockages.size());

        // Check first blockage
        Blockage firstBlockage = blockages.get(0);
        assertEquals(4, firstBlockage.getLines().size());

        // Check second blockage
        Blockage secondBlockage = blockages.get(1);
        assertEquals(4, secondBlockage.getLines().size());
        assertEquals(50, secondBlockage.getLines().get(0).getX());
        assertEquals(50, secondBlockage.getLines().get(0).getY());
    }

    @Test
    void loadBlockagesFromFile_withInvalidData_skipsInvalidLines() throws IOException {
        // Arrange
        String csvContent = "01d00h31m-01d21h35m:15,10,30,10,30,18,15,18\n" +
                "invalid-blockage-line\n" +
                "02d00h00m-02d23h59m:50,50,60,50,60,60,50,60\n";

        MockMultipartFile file = new MockMultipartFile(
                "blockages.csv",
                "blockages.csv",
                "text/csv",
                csvContent.getBytes());

        LocalDate referenceDate = LocalDate.of(2025, 1, 1);

        // Act
        List<Blockage> blockages = fileReaderService.loadBlockagesFromFile(file, referenceDate);

        // Assert
        assertEquals(2, blockages.size());
    }

    @Test
    void loadAndSaveOrders_savesToRepository() throws IOException {
        // Arrange
        String csvContent = "01d11h25m:69,49,c-001,3m3,4h\n" +
                "01d12h30m:80,120,c-002,5m3,8h\n";

        MockMultipartFile file = new MockMultipartFile(
                "orders.csv",
                "orders.csv",
                "text/csv",
                csvContent.getBytes());

        LocalDate referenceDate = LocalDate.of(2025, 1, 1);

        List<Order> orders = new ArrayList<>();
        orders.add(new Order("c-001-202501011125", LocalDateTime.of(2025, 1, 1, 11, 25),
                LocalDateTime.of(2025, 1, 1, 15, 25), 3, new Position(69, 49)));
        orders.add(new Order("c-002-202501011230", LocalDateTime.of(2025, 1, 1, 12, 30),
                LocalDateTime.of(2025, 1, 1, 20, 30), 5, new Position(80, 120)));

        when(orderRepository.saveAll(anyList())).thenReturn(orders);

        // Act
        List<Order> savedOrders = fileReaderService.loadAndSaveOrders(file, referenceDate);

        // Assert
        assertEquals(2, savedOrders.size());
        verify(orderRepository, times(1)).saveAll(anyList());
    }

    @Test
    void loadAndSaveBlockages_savesToRepository() throws IOException {
        // Arrange
        String csvContent = "01d00h31m-01d21h35m:15,10,30,10,30,18,15,18\n" +
                "02d00h00m-02d23h59m:50,50,60,50,60,60,50,60\n";

        MockMultipartFile file = new MockMultipartFile(
                "blockages.csv",
                "blockages.csv",
                "text/csv",
                csvContent.getBytes());

        LocalDate referenceDate = LocalDate.of(2025, 1, 1);

        List<Position> positions1 = new ArrayList<>();
        positions1.add(new Position(15, 10));
        positions1.add(new Position(30, 10));
        positions1.add(new Position(30, 18));
        positions1.add(new Position(15, 18));

        List<Position> positions2 = new ArrayList<>();
        positions2.add(new Position(50, 50));
        positions2.add(new Position(60, 50));
        positions2.add(new Position(60, 60));
        positions2.add(new Position(50, 60));

        List<Blockage> blockages = new ArrayList<>();
        blockages.add(new Blockage(
                LocalDateTime.of(2025, 1, 1, 0, 31),
                LocalDateTime.of(2025, 1, 1, 21, 35),
                positions1));
        blockages.add(new Blockage(
                LocalDateTime.of(2025, 1, 2, 0, 0),
                LocalDateTime.of(2025, 1, 2, 23, 59),
                positions2));

        when(blockageRepository.saveAll(anyList())).thenReturn(blockages);

        // Act
        List<Blockage> savedBlockages = fileReaderService.loadAndSaveBlockages(file, referenceDate);

        // Assert
        assertEquals(2, savedBlockages.size());
        verify(blockageRepository, times(1)).saveAll(anyList());
    }
}