package com.example.plgsystem.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;

/**
 * Enumeración que define los turnos de trabajo en el sistema.
 */
@Getter
@RequiredArgsConstructor
public enum Shift {
    T1(LocalTime.of(0, 0), LocalTime.of(7, 59)),
    T2(LocalTime.of(8, 0), LocalTime.of(15, 59)),
    T3(LocalTime.of(16, 0), LocalTime.of(23, 59));

    private final LocalTime startTime;
    private final LocalTime endTime;

    /**
     * Determina si una hora específica está dentro de este turno.
     *
     * @param time La hora a verificar
     * @return true si la hora está dentro de este turno, false en caso contrario
     */
    public boolean contains(LocalTime time) {
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    /**
     * Obtiene el turno correspondiente a una hora específica.
     *
     * @param time La hora para la cual se desea conocer el turno
     * @return El turno correspondiente
     * @throws IllegalArgumentException si no se encuentra un turno para la hora dada
     */
    public static Shift fromTime(LocalTime time) {
        for (Shift shift : Shift.values()) {
            if (shift.contains(time)) {
                return shift;
            }
        }
        throw new IllegalArgumentException("No se encontró un turno para la hora: " + time);
    }
}
