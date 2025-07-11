package com.example.plgsystem.demo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.plgsystem.enums.DepotType;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Depot;
import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.operation.VehiclePlan;
import com.example.plgsystem.orchest.SimpleDataLoader;
import com.example.plgsystem.orchest.Event;
import com.example.plgsystem.orchest.EventType;
import com.example.plgsystem.orchest.Orchestrator;
import com.example.plgsystem.simulation.SimulationState;

/**
 * Demo application to visualize vehicle planning in detail.
 * Run this class directly to see all the messages with complete details.
 * 
 * This demo now includes Orchestrator integration for advanced simulation.
 */
public class VehiclePlanDemo {

    private static final String SECTION_DIVIDER = "════════════════════════════════════════════════════════════";
    private static final String SUB_DIVIDER = "--------------------------------------------------------";

    public static void main(String[] args) {
        printHeader("🚚 ENHANCED VEHICLE PLAN DEMO WITH ORCHESTRATOR 🚚");

        // Setup the test environment
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 0, 0);
        List<Vehicle> vehicles = createTestVehicles();
        Depot mainDepot = createMainDepot();
        List<Depot> auxDepots = createAuxiliaryDepots();
        List<Order> orders = createTestOrders(currentTime);

        printSection("1️⃣ INITIALIZING SIMULATION STATE");
        SimulationState state = new SimulationState(vehicles, mainDepot, auxDepots, currentTime);
        for (Order order : orders) {
            state.addOrder(order);
        }

        System.out.println("📊 Initial State:");
        System.out.println(state);

        printSection("2️⃣ CREATING ORCHESTRATOR");
        // Create a simple in-memory data loader that returns our test orders as events
        SimpleDataLoader dataLoader = new SimpleDataLoader();
        for (Order order : orders) {
            dataLoader.addEvent(new Event(EventType.ORDER_ARRIVAL, order.getArrivalTime(), order.getId(), order));
        }
        
        // Initialize the orchestrator with 15-minute ticks and replanning every 60 minutes
        Duration tickDuration = Duration.ofMinutes(1);
        int minutesForReplan = 60;
        Orchestrator orchestrator = new Orchestrator(state, tickDuration, minutesForReplan, dataLoader);
        
        // Add additional future events if desired
        addSampleEvents(orchestrator, currentTime);
        
        System.out.println("⏱️ Orchestrator initialized with:");
        System.out.println("   - Tick duration: " + tickDuration.toMinutes() + " minutes");
        System.out.println("   - Replan interval: " + minutesForReplan + " minutes");
        System.out.println("   - Initial simulation time: " + state.getCurrentTime());
        
        printSection("4️⃣ RUNNING SIMULATION");
        
        // Define how many ticks to simulate
        int totalTicks = 1000;
        
        for (int i = 1; i <= totalTicks; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            orchestrator.advanceTick();

            if (i % 10 != 0) {
                continue;
            }

            printSubsection("TICK " + i + " OF " + totalTicks + " - Current time: " + state.getCurrentTime());
            
            System.out.println("Advancing simulation by " + tickDuration.toMinutes() + " minutes...");
            // Advance one tick
            
            System.out.println("\n📊 Updated State at " + orchestrator.getEnvironment().getCurrentTime() + ":");
            displayStateSnapshot(orchestrator.getEnvironment());
            
            System.out.println("\n🚚 Vehicle Positions:");
            displayVehiclePositions(orchestrator.getEnvironment());

            if (i % 60 == 0) {
                System.out.println("📝 Replanned vehicle plans:");
                displayVehiclePlans(orchestrator);
            }
        }
        
        printSection("5️⃣ FINAL SIMULATION STATE");
        System.out.println("📊 Final State at " + orchestrator.getEnvironment().getCurrentTime() + ":");
        System.out.println(orchestrator.getEnvironment());
        
        printSubsection("📋 Final Vehicle Plans");
        displayVehiclePlans(orchestrator);
        
        printSection("✅ SIMULATION DEMO COMPLETED");
    }
    
    private static void displayStateSnapshot(SimulationState state) {
        System.out.println("- Time: " + state.getCurrentTime());
        System.out.println("- Orders: " + state.getOrders().size());
        System.out.println("- Blockages: " + state.getBlockages().size());
        System.out.println("- Incidents: " + state.getIncidents().size());
        System.out.println("- Maintenance Tasks: " + state.getMaintenances().size());
    }
    
    private static void displayVehiclePositions(SimulationState state) {
        for (Vehicle vehicle : state.getVehicles()) {
            Position pos = vehicle.getCurrentPosition();
            System.out.printf("- %s (%s): Position (%d, %d), Status: %s, Fuel: %.2f gal, GLP: %d m³%n", 
                    vehicle.getId(), 
                    vehicle.getType(),
                    pos.getX(), 
                    pos.getY(),
                    vehicle.getStatus(),
                    vehicle.getCurrentFuelGal(),
                    vehicle.getCurrentGlpM3());
        }
    }
    
    private static void displayVehiclePlans(Orchestrator orchestrator) {
        Map<String, VehiclePlan> vehiclePlans = orchestrator.getVehiclePlans();
        
        if (vehiclePlans.isEmpty()) {
            System.out.println("❌ No vehicle plans currently active");
        } else {
            for (Map.Entry<String, VehiclePlan> entry : vehiclePlans.entrySet()) {
                String vehicleId = entry.getKey();
                Vehicle vehicle = orchestrator.getEnvironment().getVehicleById(vehicleId);
                VehiclePlan plan = entry.getValue();
                
                System.out.println("\n📋 Plan for vehicle " + vehicle.getId() + ":");
                System.out.println("   Status: " + vehicle.getStatus());
                System.out.println("   Current Position: " + vehicle.getCurrentPosition());
                System.out.println(SUB_DIVIDER);
                System.out.println(plan);
                
                if (plan.getCurrentAction() != null) {
                    System.out.println("   Current Action Progress: " + 
                            String.format("%.1f%%", plan.getCurrentAction().getCurrentProgress() * 100));
                }
            }
        }
    }
    
    private static void addSampleEvents(Orchestrator orchestrator, LocalDateTime currentTime) {
        // Example: Add a future order
        Order futureOrder = Order.builder()
                .id("ORD-FUTURE")
                .arrivalTime(currentTime.plusMinutes(45))  // Will arrive during simulation
                .deadlineTime(currentTime.plusHours(8))
                .glpRequestM3(12)
                .position(new Position(25, 25))
                .build();
                
        orchestrator.addEvent(new Event(
                EventType.ORDER_ARRIVAL,
                futureOrder.getArrivalTime(),
                futureOrder.getId(),
                futureOrder));
                
        // Example: Schedule a vehicle breakdown
        orchestrator.addEvent(new Event(
                EventType.VEHICLE_BREAKDOWN,
                currentTime.plusMinutes(60), // After 1 hour
                "TC-01",  // The TC vehicle will break down
                null));
                
        System.out.println("🗓️ Added sample events:");
        System.out.println("   - Future order arrival at " + futureOrder.getArrivalTime());
        System.out.println("   - Vehicle TC-01 breakdown at " + currentTime.plusMinutes(60));
    }

    private static void printHeader(String text) {
        System.out.println("\n" + SECTION_DIVIDER);
        System.out.println(centerText(text, SECTION_DIVIDER.length()));
        System.out.println(SECTION_DIVIDER + "\n");
    }

    private static void printSection(String title) {
        System.out.println("\n" + SECTION_DIVIDER);
        System.out.println(" " + title);
        System.out.println(SECTION_DIVIDER);
    }

    private static void printSubsection(String title) {
        System.out.println("\n" + SUB_DIVIDER);
        System.out.println(" " + title);
        System.out.println(SUB_DIVIDER);
    }

    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        if (padding <= 0)
            return text;
        return " ".repeat(padding) + text;
    }

    private static List<Vehicle> createTestVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();

        // Vehículo tipo TA
        Vehicle vehicleTA = new Vehicle(
                "TA-01",
                VehicleType.TA,
                new Position(10, 10)); // Posición cerca del centro
        vehicles.add(vehicleTA);

        // Vehículo tipo TB
        Vehicle vehicleTB = new Vehicle(
                "TB-01",
                VehicleType.TB,
                new Position(40, 40)); // Posición cerca del depósito norte
        vehicles.add(vehicleTB);

        // Vehículo tipo TC
        Vehicle vehicleTC = new Vehicle(
                "TC-01",
                VehicleType.TC,
                new Position(60, 5)); // Posición cerca del depósito este
        vehicles.add(vehicleTC);

        // Vehículo tipo TD
        Vehicle vehicleTD = new Vehicle(
                "TD-01",
                VehicleType.TD,
                new Position(35, 25)); // Posición central
        vehicles.add(vehicleTD);

        return vehicles;
    }

    private static Depot createMainDepot() {
        return new Depot(
                "MAIN-DEP",
                Constants.MAIN_DEPOT_LOCATION,
                10000,
                DepotType.MAIN);
    }

    private static List<Depot> createAuxiliaryDepots() {
        List<Depot> auxDepots = new ArrayList<>();

        // Depósito auxiliar norte
        Depot northDepot = new Depot(
                "AUX-NORTH",
                Constants.NORTH_DEPOT_LOCATION,
                5000,
                DepotType.AUXILIARY);
        northDepot.refill(); // Llenar el depósito
        auxDepots.add(northDepot);

        // Depósito auxiliar este
        Depot eastDepot = new Depot(
                "AUX-EAST",
                Constants.EAST_DEPOT_LOCATION,
                5000,
                DepotType.AUXILIARY);
        eastDepot.refill(); // Llenar el depósito
        auxDepots.add(eastDepot);

        return auxDepots;
    }

    private static List<Order> createTestOrders(LocalDateTime currentTime) {
        List<Order> orders = new ArrayList<>();

        // Orden 1 - Centro de la ciudad
        Order order1 = Order.builder()
                .id("ORD-001")
                .arrivalTime(currentTime.minusHours(2))
                .deadlineTime(currentTime.plusMinutes(1))
                .glpRequestM3(10)
                .position(new Position(20, 15))
                .build();
        orders.add(order1);

        // Orden 2 - Sur de la ciudad
        Order order2 = Order.builder()
                .id("ORD-002")
                .arrivalTime(currentTime.minusHours(1))
                .deadlineTime(currentTime.plusMinutes(2))
                .glpRequestM3(5)
                .position(new Position(30, 5))
                .build();
        orders.add(order2);

        // Orden 3 - Norte de la ciudad (cerca del depósito auxiliar norte)
        Order order3 = Order.builder()
                .id("ORD-003")
                .arrivalTime(currentTime.minusMinutes(30))
                .deadlineTime(currentTime.plusMinutes(3))
                .glpRequestM3(15)
                .position(new Position(45, 40))
                .build();
        orders.add(order3);

        // Orden 4 - Este de la ciudad (cerca del depósito auxiliar este)
        Order order4 = Order.builder()
                .id("ORD-004")
                .arrivalTime(currentTime)
                .deadlineTime(currentTime.plusMinutes(3))
                .glpRequestM3(8)
                .position(new Position(60, 10))
                .build();
        orders.add(order4);

        return orders;
    }
}