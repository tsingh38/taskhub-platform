package com.taskhub.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskhub.taskservice.dto.TaskRequest;
import com.taskhub.taskservice.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach; // Added this
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TaskControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository repository;

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // 💡 GOOD PRACTICE: Clean DB before each test to ensure isolation
    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void fullTaskLifecycle_shouldWork() throws Exception {
        // CREATE
        TaskRequest request = new TaskRequest(
                "Integration Task",
                LocalDateTime.now().plusDays(1)
        );

        String createResponse = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(createResponse).get("id").asText();

        // GET BY ID
        mockMvc.perform(get("/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Integration Task"));

        // GET ALL (PAGINATED) - ⚠️ FIXED HERE
        mockMvc.perform(get("/tasks")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "createdAt,desc")) // Optional: Test sorting too
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1)) // Check size of 'content' array
                .andExpect(jsonPath("$.content[0].title").value("Integration Task")); // Verify data

        // UPDATE
        TaskRequest update = new TaskRequest(
                "Updated Task",
                LocalDateTime.now().plusDays(2)
        );

        mockMvc.perform(put("/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Task"));

        // DELETE
        mockMvc.perform(delete("/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        // VERIFY DELETED
        mockMvc.perform(get("/tasks/{id}", taskId))
                .andExpect(status().isNotFound());
    }
}