package com.example.plgsystem.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.Position;

@Service
public class CSVService {

    // Regex to parse DDdHHhMMm format
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile("(\\d+)d(\\d+)h(\\d+)m");

    public List<Order> parseCSV(MultipartFile file) throws IOException {
        List<Order> orders = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean dataSectionStarted = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("# AGREGAR PEDIDOS AQUI:")) {
                    dataSectionStarted = true;
                    continue; // Skip this header line
                }

                if (!dataSectionStarted || line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip comments, empty lines, or lines before the data section
                }

                String[] fields = line.split(",");
                if (fields.length < 6) {
                    // Log or handle malformed line
                    System.err.println("Skipping malformed CSV line (expected at least 6 comma-separated fields): " + line);
                    continue;
                }

                try {
                    // fields[0]: DDdHHhMMm (e.g., 01d11h25m) - Arrival date/time
                    // fields[1]: Px (e.g., 69) - X coordinate
                    // fields[2]: Py (e.g., 49) - Y coordinate
                    // fields[3]: c-CC (e.g., c-001) - Client code (currently not stored in Order object)
                    // fields[4]: Qm3 (e.g., 25) - Volume
                    // fields[5]: PPh (e.g., 12) - Delivery deadline in hours

                    LocalDateTime arriveTime = parseCustomDateTimeString(fields[0].trim());
                    int x = Integer.parseInt(fields[1].trim());
                    int y = Integer.parseInt(fields[2].trim());
                    Position position = new Position(x, y);
                    // String clientCode = fields[3].trim(); // Available if needed in the future
                    int glpRequestM3 = Integer.parseInt(fields[4].trim());
                    long dueHours = Long.parseLong(fields[5].trim());
                    LocalDateTime dueTime = arriveTime.plusHours(dueHours);

                    // Generate a unique ID for the order
                    String orderId = UUID.randomUUID().toString();
                    
                    // Create the order using the Builder pattern
                    Order order = Order.builder()
                            .id(orderId)
                            .arriveTime(arriveTime)
                            .dueTime(dueTime)
                            .glpRequestM3(glpRequestM3)
                            .position(position)
                            .build();

                    orders.add(order);
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing number in CSV line: " + line + " - " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    System.err.println("Error parsing date/time in CSV line: " + line + " - " + e.getMessage());
                }
            }
        }
        return orders;
    }

    private LocalDateTime parseCustomDateTimeString(String dateTimeString) {
        Matcher matcher = DATE_TIME_PATTERN.matcher(dateTimeString);
        if (matcher.matches()) {
            int day = Integer.parseInt(matcher.group(1));
            int hour = Integer.parseInt(matcher.group(2));
            int minute = Integer.parseInt(matcher.group(3));

            LocalDateTime now = LocalDateTime.now(); // Assumes current year and month
            // Validate day for the current month if necessary, or adjust logic
            // For simplicity, using day as provided.
            // Consider potential issues if 'day' is not valid for 'now.getMonth()'
            // e.g. day 31 in a month with 30 days.
            // A more robust solution might involve taking year/month from the CSV or a fixed reference.
            return LocalDateTime.of(now.getYear(), now.getMonth(), day, hour, minute);
        } else {
            throw new IllegalArgumentException("Invalid date-time format: " + dateTimeString + ". Expected format: DDdHHhMMm");
        }
    }
}
