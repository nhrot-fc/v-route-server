package com.example.plgsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.plgsystem.model.Order;
import com.example.plgsystem.model.OrderStatus;
import com.example.plgsystem.model.Position;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CSVService {

    public List<Order> parseCSV(MultipartFile file) throws IOException {
        List<Order> orders = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false; // Saltar cabecera
                    continue;
                }
                String[] fields = line.split(",");

                Order order = new Order();
                order.setId(fields[0].trim());

                int x = Integer.parseInt(fields[2].trim());
                int y = Integer.parseInt(fields[3].trim());
                order.setPosition(new Position(x, y));

                order.setGlpRequest(Double.parseDouble(fields[4].trim()));
                order.setArriveDate(LocalDateTime.parse(fields[5].trim()));
                order.setDueDate(LocalDateTime.parse(fields[6].trim()));
                order.setStatus(OrderStatus.valueOf(fields[7].trim()));

                orders.add(order);
            }
        }
        return orders;
    }
}
