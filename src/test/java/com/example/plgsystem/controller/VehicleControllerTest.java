package com.example.plgsystem.controller;

import com.example.plgsystem.dto.VehicleDTO;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.model.Vehicle;
import com.example.plgsystem.enums.VehicleStatus;
import com.example.plgsystem.enums.VehicleType;
import com.example.plgsystem.service.VehicleService;
import com.example.plgsystem.service.ServeRecordService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VehicleController.class)
public class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleService vehicleService;

    @MockitoBean
    private ServeRecordService serveRecordService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllVehicles_WithPagination() throws Exception {
        // Given
        Position position1 = new Position(10, 20);
        Vehicle vehicle1 = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(position1)
                .build();

        Position position2 = new Position(30, 40);
        Vehicle vehicle2 = Vehicle.builder()
                .id("V002")
                .type(VehicleType.TB)
                .currentPosition(position2)
                .build();

        List<Vehicle> vehicles = Arrays.asList(vehicle1, vehicle2);
        Page<Vehicle> vehiclePage = new PageImpl<>(vehicles, PageRequest.of(0, 10), 2);
        
        // Mock the service
        when(vehicleService.findAllPaged(any(Pageable.class))).thenReturn(vehiclePage);

        // When & Then
        mockMvc.perform(get("/api/vehicles")
                .param("paginated", "true")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("V001"))
                .andExpect(jsonPath("$.content[1].id").value("V002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testGetVehiclesByType_WithPagination() throws Exception {
        // Given
        Position position1 = new Position(10, 20);
        Vehicle vehicle1 = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(position1)
                .build();

        Position position2 = new Position(30, 40);
        Vehicle vehicle2 = Vehicle.builder()
                .id("V002")
                .type(VehicleType.TA)
                .currentPosition(position2)
                .build();

        List<Vehicle> vehicles = Arrays.asList(vehicle1, vehicle2);
        Page<Vehicle> vehiclePage = new PageImpl<>(vehicles, PageRequest.of(0, 10), 2);
        
        // Mock the service
        when(vehicleService.findByTypePaged(eq(VehicleType.TA), any(Pageable.class))).thenReturn(vehiclePage);

        // When & Then
        mockMvc.perform(get("/api/vehicles")
                .param("paginated", "true")
                .param("type", "TA")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("V001"))
                .andExpect(jsonPath("$.content[1].id").value("V002"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void testGetVehicleById() throws Exception {
        // Given
        Position position = new Position(10, 20);
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        // Mock the service
        when(vehicleService.findById("V001")).thenReturn(Optional.of(vehicle));

        // When & Then
        mockMvc.perform(get("/api/vehicles/V001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("V001"))
                .andExpect(jsonPath("$.type").value(VehicleType.TA.toString()));
    }

    @Test
    public void testGetVehicleByIdNotFound() throws Exception {
        // Mock the service
        when(vehicleService.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/vehicles/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateVehicle() throws Exception {
        // Given
        Position position = new Position(10, 20);
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        VehicleDTO vehicleDTO = VehicleDTO.fromEntity(vehicle);

        // Mock the service
        when(vehicleService.save(any(Vehicle.class))).thenReturn(vehicle);

        // When & Then
        mockMvc.perform(post("/api/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vehicleDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("V001"));
    }

    @Test
    public void testUpdateVehicle() throws Exception {
        // Given
        Position position = new Position(10, 20);
        Vehicle originalVehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        Position newPosition = new Position(15, 25);
        Vehicle updatedVehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(newPosition)
                .build();
        updatedVehicle.setStatus(VehicleStatus.DRIVING);

        VehicleDTO updatedVehicleDTO = VehicleDTO.fromEntity(updatedVehicle);

        // Mock the service
        when(vehicleService.findById("V001")).thenReturn(Optional.of(originalVehicle));
        when(vehicleService.save(any(Vehicle.class))).thenReturn(updatedVehicle);

        // When & Then
        mockMvc.perform(put("/api/vehicles/V001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedVehicleDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(VehicleStatus.DRIVING.toString()));
    }

    @Test
    public void testDeleteVehicle() throws Exception {
        // Given
        String vehicleId = "V001";
        Position position = new Position(10, 20);
        Vehicle vehicle = Vehicle.builder()
                .id(vehicleId)
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        // Mock the service
        when(vehicleService.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        // When & Then
        mockMvc.perform(delete("/api/vehicles/" + vehicleId))
                .andExpect(status().isNoContent());
    }
    
    @Test
    public void testPaginationAndSorting() throws Exception {
        // Given
        Position position = new Position(10, 20);
        Vehicle vehicle = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(position)
                .build();

        List<Vehicle> vehicles = Arrays.asList(vehicle);
        Page<Vehicle> vehiclePage = new PageImpl<>(vehicles, PageRequest.of(2, 5), 15);
        
        // Mock the service
        when(vehicleService.findByMinimumGlpPaged(eq(100), any(Pageable.class))).thenReturn(vehiclePage);

        // When & Then
        mockMvc.perform(get("/api/vehicles")
                .param("paginated", "true")
                .param("minGlp", "100")
                .param("page", "2")
                .param("size", "5")
                .param("sortBy", "currentGlpM3")
                .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("V001"))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    public void testGetAllVehicles_WithoutPagination() throws Exception {
        // Given
        Position position1 = new Position(10, 20);
        Vehicle vehicle1 = Vehicle.builder()
                .id("V001")
                .type(VehicleType.TA)
                .currentPosition(position1)
                .build();

        Position position2 = new Position(30, 40);
        Vehicle vehicle2 = Vehicle.builder()
                .id("V002")
                .type(VehicleType.TB)
                .currentPosition(position2)
                .build();

        List<Vehicle> vehicles = Arrays.asList(vehicle1, vehicle2);
        
        // Mock the service
        when(vehicleService.findAll()).thenReturn(vehicles);

        // When & Then
        mockMvc.perform(get("/api/vehicles")
                .param("paginated", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("V001"))
                .andExpect(jsonPath("$[1].id").value("V002"));
    }
}
