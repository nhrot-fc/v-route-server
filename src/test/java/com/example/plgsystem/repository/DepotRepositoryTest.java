package com.example.plgsystem.repository;

import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Position;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class DepotRepositoryTest {

    @Autowired
    private DepotRepository depotRepository;

    @Test
    public void testDepotPersistence() {
        // Create a depot
        Position position = new Position(10, 20);
        Depot depot = new Depot("DEPOT-001", position, 30000.0, 5000.0, 20000.0, 3000.0);

        // Save the depot
        Depot savedDepot = depotRepository.save(depot);

        // Assert the depot was saved correctly
        assertThat(savedDepot.getId()).isNotNull();
        assertThat(savedDepot.getId()).isEqualTo("DEPOT-001");
        assertThat(savedDepot.getGlpCapacity()).isEqualTo(30000.0);
        assertThat(savedDepot.getFuelCapacity()).isEqualTo(5000.0);
        assertThat(savedDepot.getCurrentGLP()).isEqualTo(20000.0);
        assertThat(savedDepot.getCurrentFuel()).isEqualTo(3000.0);
        assertThat(savedDepot.getPosition().getX()).isEqualTo(10);
        assertThat(savedDepot.getPosition().getY()).isEqualTo(20);
    }

    @Test
    public void testFindById() {
        // Create and save a depot
        Position position = new Position(15, 25);
        Depot depot = new Depot("DEPOT-002", position, 25000.0, 4000.0, 15000.0, 2500.0);
        depotRepository.save(depot);

        // Find by ID
        Optional<Depot> foundDepot = depotRepository.findById("DEPOT-002");
        
        assertThat(foundDepot).isPresent();
        assertThat(foundDepot.get().getId()).isEqualTo("DEPOT-002");
        assertThat(foundDepot.get().getCurrentGLP()).isEqualTo(15000.0);
    }

    @Test
    public void testFindDepotsWithSufficientGLP() {
        // Create depots with different GLP levels
        Position pos1 = new Position(10, 10);
        Depot depot1 = new Depot("DEPOT-LOW", pos1, 20000.0, 3000.0, 5000.0, 1500.0);
        depotRepository.save(depot1);

        Position pos2 = new Position(20, 20);
        Depot depot2 = new Depot("DEPOT-MID", pos2, 30000.0, 4000.0, 12000.0, 2000.0);
        depotRepository.save(depot2);

        Position pos3 = new Position(30, 30);
        Depot depot3 = new Depot("DEPOT-HIGH", pos3, 40000.0, 5000.0, 25000.0, 3000.0);
        depotRepository.save(depot3);

        // Test finding depots with at least 10000 GLP
        List<Depot> sufficientDepots = depotRepository.findDepotsWithSufficientGLP(10000.0);
        
        assertThat(sufficientDepots).hasSize(2);
        assertThat(sufficientDepots.stream().map(Depot::getId))
            .containsExactlyInAnyOrder("DEPOT-MID", "DEPOT-HIGH");

        // Test finding depots with at least 20000 GLP
        List<Depot> highCapacityDepots = depotRepository.findDepotsWithSufficientGLP(20000.0);
        
        assertThat(highCapacityDepots).hasSize(1);
        assertThat(highCapacityDepots.get(0).getId()).isEqualTo("DEPOT-HIGH");

        // Test finding depots with more GLP than any depot has
        List<Depot> noneFound = depotRepository.findDepotsWithSufficientGLP(30000.0);
        assertThat(noneFound).isEmpty();
    }

    @Test
    public void testFindDepotsByLocationRange() {
        // Create depots at different locations
        Position pos1 = new Position(5, 5);
        Depot depot1 = new Depot("DEPOT-SW", pos1, 20000.0, 3000.0, 10000.0, 1500.0);
        depotRepository.save(depot1);

        Position pos2 = new Position(15, 15);
        Depot depot2 = new Depot("DEPOT-CENTER", pos2, 30000.0, 4000.0, 15000.0, 2000.0);
        depotRepository.save(depot2);

        Position pos3 = new Position(25, 25);
        Depot depot3 = new Depot("DEPOT-NE", pos3, 25000.0, 3500.0, 12000.0, 1800.0);
        depotRepository.save(depot3);

        Position pos4 = new Position(35, 5);
        Depot depot4 = new Depot("DEPOT-SE", pos4, 22000.0, 3200.0, 11000.0, 1600.0);
        depotRepository.save(depot4);

        // Test finding depots in a specific range (10-20, 10-20)
        List<Depot> depotsInRange = depotRepository.findDepotsByLocationRange(10, 20, 10, 20);
        
        assertThat(depotsInRange).hasSize(1);
        assertThat(depotsInRange.get(0).getId()).isEqualTo("DEPOT-CENTER");

        // Test finding depots in a larger range (0-30, 0-30)
        List<Depot> depotsInLargeRange = depotRepository.findDepotsByLocationRange(0, 30, 0, 30);
        
        assertThat(depotsInLargeRange).hasSize(3);
        assertThat(depotsInLargeRange.stream().map(Depot::getId))
            .containsExactlyInAnyOrder("DEPOT-SW", "DEPOT-CENTER", "DEPOT-NE");

        // Test finding depots in range that includes all
        List<Depot> allDepots = depotRepository.findDepotsByLocationRange(0, 40, 0, 30);
        
        assertThat(allDepots).hasSize(4);
    }

    @Test
    public void testGetTotalStorageCapacity() {
        // Create depots with known capacities
        Position pos1 = new Position(10, 10);
        Depot depot1 = new Depot("DEPOT-A", pos1, 20000.0, 3000.0, 10000.0, 1500.0);
        depotRepository.save(depot1);

        Position pos2 = new Position(20, 20);
        Depot depot2 = new Depot("DEPOT-B", pos2, 30000.0, 4000.0, 15000.0, 2000.0);
        depotRepository.save(depot2);

        Position pos3 = new Position(30, 30);
        Depot depot3 = new Depot("DEPOT-C", pos3, 25000.0, 3500.0, 12000.0, 1800.0);
        depotRepository.save(depot3);

        // Test total storage capacity
        Double totalCapacity = depotRepository.getTotalStorageCapacity();
        
        assertThat(totalCapacity).isNotNull();
        assertThat(totalCapacity).isEqualTo(75000.0); // 20000 + 30000 + 25000
    }

    @Test
    public void testGetCurrentTotalGLP() {
        // Create depots with known current GLP levels
        Position pos1 = new Position(10, 10);
        Depot depot1 = new Depot("DEPOT-X", pos1, 20000.0, 3000.0, 8000.0, 1500.0);
        depotRepository.save(depot1);

        Position pos2 = new Position(20, 20);
        Depot depot2 = new Depot("DEPOT-Y", pos2, 30000.0, 4000.0, 12000.0, 2000.0);
        depotRepository.save(depot2);

        Position pos3 = new Position(30, 30);
        Depot depot3 = new Depot("DEPOT-Z", pos3, 25000.0, 3500.0, 15000.0, 1800.0);
        depotRepository.save(depot3);

        // Test current total GLP
        Double currentTotalGLP = depotRepository.getCurrentTotalGLP();
        
        assertThat(currentTotalGLP).isNotNull();
        assertThat(currentTotalGLP).isEqualTo(35000.0); // 8000 + 12000 + 15000
    }

    @Test
    public void testEmptyRepository() {
        // Test queries on empty repository
        List<Depot> allDepots = depotRepository.findAll();
        assertThat(allDepots).isEmpty();

        List<Depot> sufficientDepots = depotRepository.findDepotsWithSufficientGLP(1000.0);
        assertThat(sufficientDepots).isEmpty();

        List<Depot> depotsInRange = depotRepository.findDepotsByLocationRange(0, 10, 0, 10);
        assertThat(depotsInRange).isEmpty();

        Double totalCapacity = depotRepository.getTotalStorageCapacity();
        assertThat(totalCapacity).isNull();

        Double currentTotalGLP = depotRepository.getCurrentTotalGLP();
        assertThat(currentTotalGLP).isNull();
    }

    @Test
    public void testDepotOperations() {
        // Create a depot and test business operations
        Position position = new Position(50, 50);
        Depot depot = new Depot("DEPOT-OPS", position, 10000.0, 2000.0, 5000.0, 1000.0);
        depotRepository.save(depot);

        // Test can serve operation
        assertThat(depot.canServe(3000.0)).isTrue();
        assertThat(depot.canServe(6000.0)).isFalse();

        // Test serve operation
        depot.serve(2000.0);
        assertThat(depot.getCurrentGLP()).isEqualTo(3000.0);

        // Test refill operation
        depot.refill();
        assertThat(depot.getCurrentGLP()).isEqualTo(depot.getGlpCapacity());

        // Test refill fuel operation
        depot.refillFuel();
        assertThat(depot.getCurrentFuel()).isEqualTo(depot.getFuelCapacity());

        // Save the updated depot
        Depot updatedDepot = depotRepository.save(depot);
        assertThat(updatedDepot.getCurrentGLP()).isEqualTo(10000.0);
        assertThat(updatedDepot.getCurrentFuel()).isEqualTo(2000.0);
    }
}
