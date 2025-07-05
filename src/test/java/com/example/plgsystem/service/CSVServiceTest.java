package com.example.plgsystem.service;

import com.example.plgsystem.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class CSVServiceTest {

    @InjectMocks
    private CSVService csvService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void parseCSV_withValidData_returnsOrdersList() throws IOException {
        // Arrange
        String csvContent = "# Sample order file\n" +
                "# AGREGAR PEDIDOS AQUI:\n" +
                "01d11h25m,69,49,c-001,25,12\n" +
                "01d12h30m,80,120,c-002,30,8\n";
        
        MockMultipartFile file = new MockMultipartFile(
                "orders.csv", 
                "orders.csv", 
                "text/csv", 
                csvContent.getBytes());

        // Act
        List<Order> orders = csvService.parseCSV(file);

        // Assert
        assertEquals(2, orders.size());
        
        // Check first order
        Order firstOrder = orders.get(0);
        assertEquals(25, firstOrder.getGlpRequestM3());
        assertEquals(25, firstOrder.getRemainingGlpM3());
        assertEquals(69, firstOrder.getPosition().getX());
        assertEquals(49, firstOrder.getPosition().getY());
        
        // Check second order
        Order secondOrder = orders.get(1);
        assertEquals(30, secondOrder.getGlpRequestM3());
        assertEquals(80, secondOrder.getPosition().getX());
        assertEquals(120, secondOrder.getPosition().getY());
    }

    @Test
    void parseCSV_withInvalidData_skipsInvalidLines() throws IOException {
        // Arrange
        String csvContent = "# Sample order file\n" +
                "# AGREGAR PEDIDOS AQUI:\n" +
                "01d11h25m,69,49,c-001,25,12\n" +
                "invalid line\n" +  // This line should be skipped
                "01d12h30m,80,120,c-002,30,8\n";
        
        MockMultipartFile file = new MockMultipartFile(
                "orders.csv", 
                "orders.csv", 
                "text/csv", 
                csvContent.getBytes());

        // Act
        List<Order> orders = csvService.parseCSV(file);

        // Assert
        assertEquals(2, orders.size());
        
        // Verify IDs are generated correctly
        List<String> ids = orders.stream().map(Order::getId).collect(Collectors.toList());
        assertTrue(ids.stream().allMatch(id -> id.startsWith("ORD-")));
    }

    @Test
    void parseCSV_withEmptyFile_returnsEmptyList() throws IOException {
        // Arrange
        String csvContent = "";
        MockMultipartFile file = new MockMultipartFile(
                "empty.csv", 
                "empty.csv", 
                "text/csv", 
                csvContent.getBytes());

        // Act
        List<Order> orders = csvService.parseCSV(file);

        // Assert
        assertEquals(0, orders.size());
    }

    @Test
    void parseCSV_withCommentsOnly_returnsEmptyList() throws IOException {
        // Arrange
        String csvContent = "# This is a comment\n# Another comment\n";
        MockMultipartFile file = new MockMultipartFile(
                "comments.csv", 
                "comments.csv", 
                "text/csv", 
                csvContent.getBytes());

        // Act
        List<Order> orders = csvService.parseCSV(file);

        // Assert
        assertEquals(0, orders.size());
    }

    @Test
    void parseCSV_withInvalidDateFormat_skipsThoseLinesOnly() throws IOException {
        // Arrange
        String csvContent = "# AGREGAR PEDIDOS AQUI:\n" +
                "01d11h25m,69,49,c-001,25,12\n" +
                "invalid-date,70,50,c-003,20,10\n" +  // Invalid date format
                "01d14h00m,90,100,c-004,15,24\n";
        
        MockMultipartFile file = new MockMultipartFile(
                "orders.csv", 
                "orders.csv", 
                "text/csv", 
                csvContent.getBytes());

        // Act
        List<Order> orders = csvService.parseCSV(file);

        // Assert
        assertEquals(2, orders.size());
        
        // Check positions to verify which orders were parsed correctly
        boolean hasPosition69_49 = false;
        boolean hasPosition90_100 = false;
        
        for (Order order : orders) {
            if (order.getPosition().getX() == 69 && order.getPosition().getY() == 49) {
                hasPosition69_49 = true;
            }
            if (order.getPosition().getX() == 90 && order.getPosition().getY() == 100) {
                hasPosition90_100 = true;
            }
        }
        
        assertTrue(hasPosition69_49);
        assertTrue(hasPosition90_100);
    }
} 