package com.example.plgsystem.repository;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class DepotRepositoryTest {

    @Autowired
    private DepotRepository depotRepository;

    @Test
    public void testSaveAndFindById() {
        // Given
        Position position = new Position(10, 20);
        Depot depot = new Depot("D001", position, 1000, true);

        // When
        depotRepository.save(depot);
        Optional<Depot> foundDepot = depotRepository.findById("D001");

        // Then
        assertTrue(foundDepot.isPresent());
        assertEquals("D001", foundDepot.get().getId());
        assertEquals(position.getX(), foundDepot.get().getPosition().getX());
        assertEquals(position.getY(), foundDepot.get().getPosition().getY());
        assertEquals(1000, foundDepot.get().getGlpCapacityM3());
        assertTrue(foundDepot.get().isCanRefuel());
    }

    @Test
    public void testFindAll() {
        // Given
        Depot depot1 = new Depot("D001", new Position(10, 20), 1000, true);
        Depot depot2 = new Depot("D002", new Position(30, 40), 2000, false);
        
        depotRepository.save(depot1);
        depotRepository.save(depot2);

        // When
        List<Depot> depots = depotRepository.findAll();

        // Then
        assertEquals(2, depots.size());
    }

    @Test
    public void testDeleteById() {
        // Given
        Depot depot = new Depot("D001", new Position(10, 20), 1000, true);
        depotRepository.save(depot);

        // When
        depotRepository.deleteById("D001");
        Optional<Depot> foundDepot = depotRepository.findById("D001");

        // Then
        assertFalse(foundDepot.isPresent());
    }

    @Test
    public void testFindByCanRefuel() {
        // Given
        Depot depot1 = new Depot("D001", new Position(10, 20), 1000, true);
        Depot depot2 = new Depot("D002", new Position(30, 40), 2000, false);
        Depot depot3 = new Depot("D003", new Position(50, 60), 1500, true);
        
        depotRepository.saveAll(List.of(depot1, depot2, depot3));

        // When - assuming there's a finder method for this property
        List<Depot> refuelingDepots = depotRepository.findByCanRefuel(true);

        // Then
        assertEquals(2, refuelingDepots.size());
    }
}
