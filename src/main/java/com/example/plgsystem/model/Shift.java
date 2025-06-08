package com.example.plgsystem.model;

import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "shifts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Shift {
    @Id
    private String id; // T1, T2, T3
    
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public boolean isTimeInShift(LocalTime time) {
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }

    // Static instances for predefined shifts
    public static final Shift T1 = new Shift("T1", LocalTime.of(0, 0), LocalTime.of(8, 0));
    public static final Shift T2 = new Shift("T2", LocalTime.of(8, 0), LocalTime.of(16, 0));
    public static final Shift T3 = new Shift("T3", LocalTime.of(16, 0), LocalTime.MIDNIGHT);

    public static Shift getShiftForTime(LocalTime time) {
        if (T1.isTimeInShift(time)) return T1;
        if (T2.isTimeInShift(time)) return T2;
        if (!time.isBefore(T3.getStartTime()) && T3.getEndTime().equals(LocalTime.MIDNIGHT)) { // After or at 16:00
            return T3;
        }
        return null; // Or throw an exception if time is somehow outside 00:00-23:59:59
    }
}
