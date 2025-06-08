package com.example.plgsystem.controller;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.repository.BlockageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class BlockageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BlockageRepository blockageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Blockage testBlockage;

    @BeforeEach
    public void setup() {
        // Create a test blockage using the proper constructor
        Position startNode = new Position(30, 40);
        Position endNode = new Position(35, 45);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusHours(3);
        
        testBlockage = new Blockage(startNode, endNode, now, end);
        testBlockage = blockageRepository.save(testBlockage);
    }

    @Test
    public void testGetAllBlockages() throws Exception {
        mockMvc.perform(get("/api/blockages"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    public void testGetBlockageById() throws Exception {
        mockMvc.perform(get("/api/blockages/{id}", testBlockage.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testBlockage.getId().intValue())));
        
        // Test with non-existent ID
        mockMvc.perform(get("/api/blockages/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetActiveBlockages() throws Exception {
        // Add one future blockage
        Position futureStartNode = new Position(50, 60);
        Position futureEndNode = new Position(55, 65);
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        
        Blockage futureBlockage = new Blockage(futureStartNode, futureEndNode, future, future.plusHours(2));
        blockageRepository.save(futureBlockage);
        
        mockMvc.perform(get("/api/blockages/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1)))); // Only the current blockage is active
    }

    @Test
    public void testGetActiveBlockagesAt() throws Exception {
        // Test at current time (should find our test blockage)
        String currentTime = LocalDateTime.now().toString();
        
        mockMvc.perform(get("/api/blockages/active/{dateTime}", currentTime))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))));
        
        // Test at future time (should find none)
        String futureTime = LocalDateTime.now().plusDays(2).toString();
        
        mockMvc.perform(get("/api/blockages/active/{dateTime}", futureTime))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testGetBlockagesByDateRange() throws Exception {
        // Create a blockage for tomorrow
        Position tomorrowStartNode = new Position(60, 70);
        Position tomorrowEndNode = new Position(65, 75);
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        LocalDateTime tomorrowEnd = tomorrow.plusHours(2);
        
        Blockage tomorrowBlockage = new Blockage(tomorrowStartNode, tomorrowEndNode, tomorrow, tomorrowEnd);
        blockageRepository.save(tomorrowBlockage);
        
        // Test range including today and tomorrow
        String startDate = LocalDateTime.now().minusHours(1).toString();
        String endDate = LocalDateTime.now().plusDays(2).toString();
        
        mockMvc.perform(get("/api/blockages/date-range")
                .param("startDate", startDate)
                .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(2))));
        
        // Test range for only tomorrow
        String tomorrowStart = tomorrow.minusHours(1).toString();
        String tomorrowEndStr = tomorrow.plusHours(3).toString();
        
        mockMvc.perform(get("/api/blockages/date-range")
                .param("startDate", tomorrowStart)
                .param("endDate", tomorrowEndStr))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(equalTo(1))));
    }

    @Test
    public void testGetBlockagesForSegment() throws Exception {
        // Our test blockage has startNode at (30,40) and endNode at (35,45)
        // Test segment that matches the blockage exactly
        mockMvc.perform(get("/api/blockages/segment")
                .param("x1", "30")
                .param("y1", "40")
                .param("x2", "35")
                .param("y2", "45"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
        
        // Test segment far from blockage
        mockMvc.perform(get("/api/blockages/segment")
                .param("x1", "100")
                .param("y1", "100")
                .param("x2", "110")
                .param("y2", "110"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testCreateBlockage() throws Exception {
        // Create a new blockage
        Position newStartNode = new Position(80, 90);
        Position newEndNode = new Position(85, 95);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusHours(5);
        
        Blockage newBlockage = new Blockage(newStartNode, newEndNode, now, end);
        
        mockMvc.perform(post("/api/blockages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBlockage)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    public void testDeleteBlockage() throws Exception {
        mockMvc.perform(delete("/api/blockages/{id}", testBlockage.getId()))
                .andExpect(status().isOk());
        
        // Verify it's deleted
        mockMvc.perform(get("/api/blockages/{id}", testBlockage.getId()))
                .andExpect(status().isNotFound());
        
        // Try to delete non-existent blockage
        mockMvc.perform(delete("/api/blockages/9999"))
                .andExpect(status().isNotFound());
    }
}
