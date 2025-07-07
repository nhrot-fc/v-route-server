package com.example.plgsystem.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeParser {
    // Regex to parse DDdHHhMMm format
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile("(\\d+)d(\\d+)h(\\d+)m");
    
    public static LocalDateTime parseDateTime(int referenceYear, int referenceMonth, String dateTimeString) {
        // 01d00h31m -> day: 1, hour: 0, minute: 31
        Matcher matcher = DATE_TIME_PATTERN.matcher(dateTimeString);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid date time format: " + dateTimeString);
        }

        int day = Integer.parseInt(matcher.group(1));
        int hour = Integer.parseInt(matcher.group(2));
        int minute = Integer.parseInt(matcher.group(3));

        return LocalDateTime.of(referenceYear, referenceMonth, day, hour, minute);
    }

    public static LocalDateTime parseDateTime(LocalDate referenceDate, String dateTimeString) {
        // 01d00h31m -> day: 1, hour: 0, minute: 31
        Matcher matcher = DATE_TIME_PATTERN.matcher(dateTimeString);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid date time format: " + dateTimeString);
        }

        int day = Integer.parseInt(matcher.group(1));
        int hour = Integer.parseInt(matcher.group(2));
        int minute = Integer.parseInt(matcher.group(3));

        return LocalDateTime.of(referenceDate.getYear(), referenceDate.getMonth(), day, hour, minute);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return String.format("%02dd%02dh%02dm", dateTime.getDayOfMonth(), dateTime.getHour(), dateTime.getMinute());
    }
}
