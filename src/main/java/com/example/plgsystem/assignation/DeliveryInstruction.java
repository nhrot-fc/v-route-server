package com.example.plgsystem.assignation;

import java.time.LocalDateTime;
import java.util.Objects;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;

public class DeliveryInstruction {
    private final Order originalOrder;
    private final int glpAmountToDeliver;

    public DeliveryInstruction(Order originalOrder, int glpAmountToDeliver) {
        Objects.requireNonNull(originalOrder, "Original order cannot be null.");
        if (glpAmountToDeliver <= 0) {
            throw new IllegalArgumentException("GLP amount to deliver must be positive.");
        }
        this.originalOrder = originalOrder;
        this.glpAmountToDeliver = glpAmountToDeliver;
    }

    public Order getOriginalOrder() {
        return originalOrder;
    }

    public String getOrderId() {
        return originalOrder.getId();
    }

    public Position getCustomerPosition() {
        return originalOrder.getPosition();
    }

    public LocalDateTime getDueDate() {
        return originalOrder.getDueTime();
    }

    public int getGlpAmountToDeliver() {
        return glpAmountToDeliver;
    }

    @Override
    public String toString() {
        return "DeliveryInstruction{" +
                "orderId='" + getOrderId() +
                ", customerPosition=" + getCustomerPosition() +
                ", glpAmountToDeliver=" + glpAmountToDeliver +
                ", dueDate=" + getDueDate() +
                '}';
    }
}
