package com.example.plgsystem.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Registro de entrega de GLP realizado por un vehículo.
 * Esta clase no se persiste en base de datos, es solo usada en memoria.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServeRecord {
    private String vehicleId;
    private String orderId;
    private double servedGlpM3;
    private LocalDateTime serveTime;

    @Override
    public String toString() {
        return String.format("📝 %s→%s [GLP:%.1f m³] 🕒 %s",
                vehicleId,
                orderId,
                servedGlpM3,
                serveTime.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
    }
}
