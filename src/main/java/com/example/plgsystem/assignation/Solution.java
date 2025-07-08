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
}
