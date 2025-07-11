package com.example.plgsystem.assignation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

@Getter
public class Solution {
    private final Map<String, Integer> ordersState;
    private final Map<String, Integer> depotsState;
    private final Map<String, Route> routes;
    private final double cost;

    public Solution(Map<String, Integer> ordersState, Map<String, Integer> depotsState, Map<String, Route> routes) {
        this.ordersState = ordersState;
        this.depotsState = depotsState;
        this.routes = routes;
        this.cost = 0;
    }

    public Solution(Map<String, Integer> ordersState, Map<String, Integer> depotsState, Map<String, Route> routes,
            double cost) {
        this.ordersState = ordersState;
        this.depotsState = depotsState;
        this.routes = routes;
        this.cost = cost;
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
        sb.append(String.format("💡 Solution { 💰 cost: %.2f }\n", cost));
        
        // Orders state
        sb.append("📦 Orders State: {\n");
        if (ordersState.isEmpty()) {
            sb.append("  Empty\n");
        } else {
            for (Map.Entry<String, Integer> entry : ordersState.entrySet()) {
                sb.append(String.format("  🔖 %s: %d m³\n", entry.getKey(), entry.getValue()));
            }
        }
        sb.append("}\n");
        
        // Depots state
        sb.append("🏭 Depots State: {\n");
        if (depotsState.isEmpty()) {
            sb.append("  Empty\n");
        } else {
            for (Map.Entry<String, Integer> entry : depotsState.entrySet()) {
                sb.append(String.format("  🏢 %s: %d m³\n", entry.getKey(), entry.getValue()));
            }
        }
        sb.append("}\n");
        
        // Routes
        sb.append("🛣️ Routes: {\n");
        if (routes.isEmpty()) {
            sb.append("  Empty\n");
        } else {
            for (Map.Entry<String, Route> entry : routes.entrySet()) {
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
