package com.example.plgsystem.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BlockageTest {

    private Blockage blockage;
    private final LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 10, 0);
    private final LocalDateTime endTime = LocalDateTime.of(2025, 1, 1, 18, 0);
    private final List<Position> lines = Arrays.asList(
        new Position(10, 10),
        new Position(20, 10),
        new Position(20, 20),
        new Position(10, 20)
    );

    @BeforeEach
    public void setUp() {
        blockage = new Blockage(startTime, endTime, lines);
    }

    @Test
    public void testBlockageCreation() {
        assertNotNull(blockage);
        assertEquals(startTime, blockage.getStartTime());
        assertEquals(endTime, blockage.getEndTime());
        
        List<Position> retrievedLines = blockage.getLines();
        assertEquals(lines.size(), retrievedLines.size());
        for (int i = 0; i < lines.size(); i++) {
            assertEquals(lines.get(i).getX(), retrievedLines.get(i).getX());
            assertEquals(lines.get(i).getY(), retrievedLines.get(i).getY());
        }
    }

    @Test
    public void testGetBlockagePoints() {
        
        // For horizontal line segments
        // Line from (10,10) to (20,10) should have 11 points: (10,10), (11,10), ... (20,10)
        for (int x = 10; x <= 20; x++) {
            assertTrue(blockage.posicionEstaBloqueada(new Position(x, 10)));
        }
        
        // Line from (10,20) to (20,20) should have 11 points
        for (int x = 10; x <= 20; x++) {
            assertTrue(blockage.posicionEstaBloqueada(new Position(x, 20)));
        }
        
        // Line from (20,10) to (20,20) should have 11 points
        for (int y = 10; y <= 20; y++) {
            assertTrue(blockage.posicionEstaBloqueada(new Position(20, y)));
        }
        
        // Points outside the blockage should not be included
        assertFalse(blockage.posicionEstaBloqueada(new Position(15, 15))); // Inside the rectangle but not on border
        assertFalse(blockage.posicionEstaBloqueada(new Position(9, 10)));  // Just outside
        assertFalse(blockage.posicionEstaBloqueada(new Position(21, 10))); // Just outside
    }

    @Test
    public void testIsActiveAt() {
        // Time within the blockage period
        LocalDateTime duringBlockage = LocalDateTime.of(2025, 1, 1, 14, 0);
        assertTrue(blockage.isActiveAt(duringBlockage));
        
        // At the exact boundaries
        assertTrue(blockage.isActiveAt(startTime));
        assertTrue(blockage.isActiveAt(endTime));
        
        // Before and after the blockage
        LocalDateTime beforeBlockage = startTime.minusSeconds(1);
        LocalDateTime afterBlockage = endTime.plusSeconds(1);
        
        assertFalse(blockage.isActiveAt(beforeBlockage));
        assertFalse(blockage.isActiveAt(afterBlockage));
    }

    @Test
    public void testFromFileFormat() {
        // Base date for relative time calculation
        LocalDateTime baseDate = LocalDateTime.of(2025, 1, 1, 0, 0);
        
        // Valid format string "01d10h00m-01d18h00m:10,10,20,10,20,20,10,20"
        String validFormatString = "01d10h00m-01d18h00m:10,10,20,10,20,20,10,20";
        
        Blockage fromFile = Blockage.fromFileFormat(validFormatString, baseDate);
        
        assertNotNull(fromFile);
        assertEquals(LocalDateTime.of(2025, 1, 1, 10, 0), fromFile.getStartTime());
        assertEquals(LocalDateTime.of(2025, 1, 1, 18, 0), fromFile.getEndTime());
        
        List<Position> fileLines = fromFile.getLines();
        assertEquals(4, fileLines.size());
        assertEquals(10, fileLines.get(0).getX());
        assertEquals(10, fileLines.get(0).getY());
        assertEquals(20, fileLines.get(1).getX());
        assertEquals(10, fileLines.get(1).getY());
        
        // Invalid format (missing colon)
        String invalidFormat = "01d10h00m-01d18h00m10,10,20,10";
        assertThrows(IllegalArgumentException.class, () -> {
            Blockage.fromFileFormat(invalidFormat, baseDate);
        });
        
        // Invalid format (odd number of coordinates)
        String invalidCoordinates = "01d10h00m-01d18h00m:10,10,20";
        assertThrows(IllegalArgumentException.class, () -> {
            Blockage.fromFileFormat(invalidCoordinates, baseDate);
        });
    }

    @Test
    public void testSerializationAndDeserialization() {
        // This test checks the internal serialization/deserialization mechanism
        // We'll create a new blockage, then trigger postLoad to simulate loading from DB
        
        // The linePoints field should contain a serialized representation of the lines
        String linePoints = blockage.getLinePoints();
        assertNotNull(linePoints);
        assertTrue(linePoints.contains("10,10"));
        assertTrue(linePoints.contains("20,10"));
        
        // Create a new blockage with the same linePoints but no lines
        Blockage newBlockage = new Blockage();
        newBlockage.setId(1L); // Just to avoid null ID
        
        // Set the serialized linePoints
        try {
            java.lang.reflect.Field linePointsField = Blockage.class.getDeclaredField("linePoints");
            linePointsField.setAccessible(true);
            linePointsField.set(newBlockage, linePoints);
            
            // Now trigger postLoad to deserialize
            java.lang.reflect.Method postLoadMethod = Blockage.class.getDeclaredMethod("postLoad");
            postLoadMethod.setAccessible(true);
            postLoadMethod.invoke(newBlockage);
            
            // Check if lines were properly deserialized
            List<Position> deserializedLines = newBlockage.getLines();
            assertNotNull(deserializedLines);
            assertEquals(lines.size(), deserializedLines.size());
            
            for (int i = 0; i < lines.size(); i++) {
                assertEquals(lines.get(i).getX(), deserializedLines.get(i).getX());
                assertEquals(lines.get(i).getY(), deserializedLines.get(i).getY());
            }
            
        } catch (Exception e) {
            fail("Exception during reflection: " + e.getMessage());
        }
    }

    @Test
    public void testSetId() {
        Long id = 123L;
        blockage.setId(id);
        assertEquals(id, blockage.getId());
    }
} 