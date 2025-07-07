package com.example.plgsystem.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BlockageTest {

    @Test
    public void testBlockageCreation() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 1, 5, 0, 0);
        List<Position> blockageLine = Arrays.asList(
                new Position(10, 10),
                new Position(10, 20),
                new Position(20, 20));

        // When
        Blockage blockage = new Blockage(startTime, endTime, blockageLine);

        // Then
        assertEquals(startTime, blockage.getStartTime());
        assertEquals(endTime, blockage.getEndTime());

        // Verify that lines are correctly stored
        List<Position> retrievedLines = blockage.getLines();
        assertEquals(blockageLine.size(), retrievedLines.size());
        for (int i = 0; i < blockageLine.size(); i++) {
            assertEquals(blockageLine.get(i), retrievedLines.get(i));
        }

        // Verify serialization
        String expectedSerializedFormat = "10,10,10,20,20,20";
        assertEquals(expectedSerializedFormat, blockage.getLinePoints());
    }

    @Test
    public void testIsActiveAt() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 1, 5, 0, 0);
        List<Position> blockageLine = Arrays.asList(
                new Position(10, 10),
                new Position(10, 20));
        Blockage blockage = new Blockage(startTime, endTime, blockageLine);

        // Then
        assertTrue(blockage.isActiveAt(startTime)); // Exactly at start
        assertTrue(blockage.isActiveAt(endTime)); // Exactly at end
        assertTrue(blockage.isActiveAt(LocalDateTime.of(2025, 1, 3, 0, 0))); // Middle
        assertFalse(blockage.isActiveAt(startTime.minusDays(1))); // Before start
        assertFalse(blockage.isActiveAt(endTime.plusDays(1))); // After end
    }

    @Test
    public void testSerializeAndDeserializePositions() {
        // Given
        List<Position> positions = Arrays.asList(
                new Position(1, 2),
                new Position(3, 4),
                new Position(5, 6));

        // When
        String serialized = Blockage.serializePositions(positions);
        List<Position> deserialized = Blockage.deserializePositions(serialized);

        // Then
        assertEquals(positions.size(), deserialized.size());
        for (int i = 0; i < positions.size(); i++) {
            assertEquals(positions.get(i), deserialized.get(i));
        }
    }

    @Test
    public void testDeserializeEmptyOrNullString() {
        assertTrue(Blockage.deserializePositions("").isEmpty());
        assertTrue(Blockage.deserializePositions(null).isEmpty());
    }

    @Test
    public void testIsPositionBlocked() {
        // Given
        List<Position> blockageLine = Arrays.asList(
                new Position(10, 10),
                new Position(10, 20), // Vertical line from (10,10) to (10,20)
                new Position(20, 20) // Horizontal line from (10,20) to (20,20)
        );
        LocalDateTime now = LocalDateTime.now();
        Blockage blockage = new Blockage(now.minusDays(1), now.plusDays(1), blockageLine);

        // Test vertical segment
        assertTrue(blockage.isPositionBlocked(new Position(10, 15))); // On vertical line
        assertFalse(blockage.isPositionBlocked(new Position(11, 15))); // 1 unit right of vertical line

        // Test horizontal segment
        assertTrue(blockage.isPositionBlocked(new Position(15, 20))); // On horizontal line
        assertFalse(blockage.isPositionBlocked(new Position(15, 21))); // 1 unit above horizontal line

        // Test endpoints
        assertTrue(blockage.isPositionBlocked(new Position(10, 10))); // Start point
        assertTrue(blockage.isPositionBlocked(new Position(10, 20))); // Corner point
        assertTrue(blockage.isPositionBlocked(new Position(20, 20))); // End point

        // Test point not on any segment
        assertFalse(blockage.isPositionBlocked(new Position(5, 5)));
    }

    @Test
    public void testGetLinesAfterLoad() {
        // Create a blockage without explicitly setting lines list, only linePoints
        // string
        Blockage blockage = new Blockage();
        blockage.setId(java.util.UUID.randomUUID());
        blockage.setStartTime(LocalDateTime.now());
        blockage.setEndTime(LocalDateTime.now().plusDays(1));
        blockage.setLinePoints("5,5,10,5,10,10");

        // Get lines should deserialize from linePoints
        List<Position> lines = blockage.getLines();
        assertNotNull(lines);
        assertEquals(3, lines.size());
        assertEquals(new Position(5, 5), lines.get(0));
        assertEquals(new Position(10, 5), lines.get(1));
        assertEquals(new Position(10, 10), lines.get(2));
    }
}