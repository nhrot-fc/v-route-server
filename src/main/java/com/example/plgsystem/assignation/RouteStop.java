package com.example.plgsystem.assignation;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class RouteStop {
    private final boolean isOrderStop;
    private final String orderId;
    private final LocalDateTime orderDeadlineTime;
    private final String depotId;
    private final int glpDeliverM3;
    private final int glpLoadM3;

    // Constructor para paradas de entrega (orden)
    public RouteStop(String orderId, LocalDateTime orderDeadlineTime, int glpDeliverM3) {
        this.isOrderStop = true;
        this.orderId = orderId;
        this.orderDeadlineTime = orderDeadlineTime;
        this.glpDeliverM3 = glpDeliverM3;
        this.depotId = null;
        this.glpLoadM3 = 0;
    }

    // Constructor para paradas de carga (depósito)
    public RouteStop(String depotId, int glpLoadM3) {
        this.isOrderStop = false;
        this.depotId = depotId;
        this.glpLoadM3 = glpLoadM3;
        this.orderId = null;
        this.orderDeadlineTime = null;
        this.glpDeliverM3 = 0;
    }
    
    // Constructor para compatibilidad con código anterior
    public RouteStop(boolean isOrderStop, String orderId, String depotId, int glpDeliverM3, int glpLoadM3) {
        this.isOrderStop = isOrderStop;
        this.orderId = orderId;
        this.depotId = depotId;
        this.glpDeliverM3 = glpDeliverM3;
        this.glpLoadM3 = glpLoadM3;
        this.orderDeadlineTime = null; // Este constructor no incluye deadlineTime
    }
}
