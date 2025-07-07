package com.example.plgsystem.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;

@Getter
@RequiredArgsConstructor
public enum Shift {
    T1(LocalTime.of(0, 0), LocalTime.of(7, 59)),
    T2(LocalTime.of(8, 0), LocalTime.of(15, 59)),
    T3(LocalTime.of(16, 0), LocalTime.of(23, 59));

    private final LocalTime startTime;
    private final LocalTime endTime;

    public boolean contains(LocalTime time) {
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    public static Shift fromTime(LocalTime time) {
        for (Shift shift : Shift.values()) {
            if (shift.contains(time)) {
                return shift;
            }
        }
        throw new IllegalArgumentException("No shift found for time: " + time);
    }
}
