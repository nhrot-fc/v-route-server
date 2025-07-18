package com.example.plgsystem.assignation;

public record SolutionCost(double timeCost, double distanceCost, double lateDeliveryCost, double incompleteOrderCost,
        double invalidCost) {
    public double totalCost() {
        return timeCost() + distanceCost() + lateDeliveryCost() + incompleteOrderCost() + invalidCost();
    }

    @Override
    public final String toString() {
        return String.format(
                "{timeCost: %.6f, distanceCost: %.6f, lateDeliveryCost: %.6f, incompleteOrderCost: %.6f, invalidCost: %.6f}",
                timeCost,
                distanceCost,
                lateDeliveryCost,
                incompleteOrderCost,
                invalidCost);
    }
}
