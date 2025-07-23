package com.example.plgsystem.assignation;

import java.time.LocalDateTime;
import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Position;

import lombok.Getter;

@Getter
public class RouteStop {
    private final boolean isOrderStop;
    private final boolean isMaintenanceStop;
    private final Position position;

    private final String orderId;
    private final LocalDateTime orderDeadlineTime;
    private final int glpDeliverM3;

    private final String depotId;
    private final int glpLoadM3;

    private final LocalDateTime maintenanceTime;

    // Constructor para paradas de entrega (orden)
    public RouteStop(Position position, String orderId, LocalDateTime orderDeadlineTime, int glpDeliverM3) {
        this.position = position;
        this.isOrderStop = true;
        this.isMaintenanceStop = false;
        this.orderId = orderId;
        this.orderDeadlineTime = orderDeadlineTime;
        this.glpDeliverM3 = glpDeliverM3;
        this.depotId = null;
        this.glpLoadM3 = 0;
        this.maintenanceTime = null;
    }

    // Constructor para paradas de carga (depósito)
    public RouteStop(Position position, String depotId, int glpLoadM3) {
        this.position = position;
        this.isOrderStop = false;
        this.isMaintenanceStop = false;
        this.depotId = depotId;
        this.glpLoadM3 = glpLoadM3;
        this.orderId = null;
        this.orderDeadlineTime = null;
        this.glpDeliverM3 = 0;
        this.maintenanceTime = null;
    }

    // Constructor para paradas de mantenimiento
    public RouteStop(Position position, String depotId, LocalDateTime maintenanceTime) {
        this.position = position;
        this.isOrderStop = false;
        this.isMaintenanceStop = true;
        this.depotId = depotId;
        this.glpLoadM3 = 0;
        this.orderId = null;
        this.orderDeadlineTime = null;
        this.glpDeliverM3 = 0;
        this.maintenanceTime = maintenanceTime;
    }

    @Override
    public String toString() {
        String baseInfo;
        if (isOrderStop) {
            baseInfo = String.format("🏪 OrderStop { 📍 pos: %s, 🔖 orderId: %s, ⏰ deadline: %s, 🛢️ deliver: %d m³",
                    position, orderId, 
                    orderDeadlineTime != null ? orderDeadlineTime.format(Constants.DATE_TIME_FORMATTER) : "N/A", 
                    glpDeliverM3);
        } else if (isMaintenanceStop) {
            baseInfo = String.format("🔧 MaintenanceStop { 📍 pos: %s, 🏢 depotId: %s, ⏰ scheduled: %s",
                    position, depotId, 
                    maintenanceTime != null ? maintenanceTime.format(Constants.DATE_TIME_FORMATTER) : "N/A");
        } else {
            baseInfo = String.format("🏭 DepotStop { 📍 pos: %s, 🏢 depotId: %s, 🛢️ load: %d m³",
                    position, depotId, glpLoadM3);
        }
        baseInfo += "}";
        return baseInfo;
    }
}
