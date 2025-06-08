package com.example.plgsystem.repository;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Position;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class BlockageRepositoryTest {

    @Autowired
    private BlockageRepository blockageRepository;

    @Test
    public void testBlockagePersistence() {
        // Create a blockage using the correct constructor
        Position startNode = new Position(10, 20);
        Position endNode = new Position(15, 25);
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(2);
        Blockage blockage = new Blockage(startNode, endNode, startTime, endTime);

        // Save the blockage
        Blockage savedBlockage = blockageRepository.save(blockage);

        // Assert the blockage was saved correctly
        assertThat(savedBlockage.getId()).isNotNull();
        assertThat(savedBlockage.getStartNode().getX()).isEqualTo(10);
        assertThat(savedBlockage.getStartNode().getY()).isEqualTo(20);
        assertThat(savedBlockage.getEndNode().getX()).isEqualTo(15);
        assertThat(savedBlockage.getEndNode().getY()).isEqualTo(25);
        assertThat(savedBlockage.getStartTime()).isEqualTo(startTime);
        assertThat(savedBlockage.getEndTime()).isEqualTo(endTime);
    }

    @Test
    public void testFindActiveBlockages() {
        // Create and save active blockage
        Position startNode1 = new Position(10, 20);
        Position endNode1 = new Position(15, 25);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastStart = now.minusHours(1);
        LocalDateTime futureEnd = now.plusHours(1);
        
        Blockage activeBlockage = new Blockage(startNode1, endNode1, pastStart, futureEnd);
        blockageRepository.save(activeBlockage);

        // Create and save inactive blockage (past)
        Position startNode2 = new Position(30, 40);
        Position endNode2 = new Position(35, 45);
        LocalDateTime pastStart2 = now.minusHours(3);
        LocalDateTime pastEnd = now.minusHours(2);
        
        Blockage pastBlockage = new Blockage(startNode2, endNode2, pastStart2, pastEnd);
        blockageRepository.save(pastBlockage);

        // Find active blockages
        var activeBlockages = blockageRepository.findActiveBlockages(now);
        
        // Assert we only have one active blockage
        assertThat(activeBlockages).hasSize(1);
        assertThat(activeBlockages.get(0).getId()).isEqualTo(activeBlockage.getId());
    }

    @Test
    public void testFindBlockagesByDateRange() {
        // Create a blockage
        LocalDateTime now = LocalDateTime.now();
        
        // Create blockage within date range
        Position startNode1 = new Position(55, 55);
        Position endNode1 = new Position(60, 60);
        Blockage blockage1 = new Blockage(startNode1, endNode1, now.minusHours(1), now.plusHours(1));
        blockageRepository.save(blockage1);
        
        // Create blockage outside date range (in the past)
        Position startNode2 = new Position(80, 80);
        Position endNode2 = new Position(85, 85);
        Blockage blockage2 = new Blockage(startNode2, endNode2, now.minusDays(1), now.minusHours(3));
        blockageRepository.save(blockage2);
        
        // Find blockages within date range
        var blockagesInRange = blockageRepository.findBlockagesByDateRange(now.minusHours(2), now.plusHours(2));
        
        // Assert we only find the blockage within the date range
        assertThat(blockagesInRange).hasSize(1);
        assertThat(blockagesInRange.get(0).getId()).isEqualTo(blockage1.getId());
    }
}
