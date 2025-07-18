package com.example.plgsystem.assignation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.plgsystem.simulation.SimulationState;

import lombok.Getter;

@Getter
public class Solution {
    private final Map<String, Route> routes;
    private final SolutionCost cost;

    public Solution(Map<String, Route> routes, SimulationState state) {
        this.routes = routes;
        this.cost = SolutionEvaluator.evaluate(this, state);
    }

    public Map<String, List<DeliveryPart>> getVehicleOrderAssignments() {
        Map<String, List<DeliveryPart>> assignments = new HashMap<>();

        for (Map.Entry<String, Route> entry : this.getRoutes().entrySet()) {
            String vehicleId = entry.getKey();
            Route route = entry.getValue();
            List<DeliveryPart> deliveryParts = new ArrayList<>();

            for (RouteStop stop : route.stops()) {
                if (stop.isOrderStop()) {
                    deliveryParts.add(new DeliveryPart(
                            stop.getOrderId(),
                            stop.getGlpDeliverM3(),
                            stop.getOrderDeadlineTime()));
                }
            }

            assignments.put(vehicleId, deliveryParts);
        }

        return assignments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("💡 Solution\n");
        sb.append("\t💰 Cost:");
        sb.append(cost).append("\n");

        // Routes
        sb.append("🛣️ Routes: {\n");
        if (routes.isEmpty()) {
            sb.append("  Empty\n");
        } else {
            for (Map.Entry<String, Route> entry : routes.entrySet()) {
                if (entry.getValue() == null) {
                    sb.append(String.format("No route found for vehicle %s\n", entry.getKey()));
                    continue;
                }

                sb.append(String.format("  🚚 %s:\n", entry.getKey()));

                // Indent each line of the route's toString
                String[] routeLines = entry.getValue().toString().split("\n");
                for (String line : routeLines) {
                    sb.append("    ").append(line).append("\n");
                }
            }
        }
        sb.append("}\n");

        return sb.toString();
    }
}
