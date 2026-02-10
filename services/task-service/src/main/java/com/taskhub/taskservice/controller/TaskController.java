package com.taskhub.taskservice.controller;

import com.taskhub.taskservice.dto.TaskRequest;
import com.taskhub.taskservice.dto.TaskResponse;
import com.taskhub.taskservice.service.TaskService;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@Tag(name = "Tasks", description = "Task management operations")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);
    private final TaskService taskService;

    public TaskController(TaskService taskService, MeterRegistry registry) {
        this.taskService = taskService;
    }

    @Operation(summary = "Create a task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Task created"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Timed(value = "api.tasks.create", description = "Time taken to create a task",histogram = true)
    public TaskResponse create(@Valid @RequestBody TaskRequest request) {
            log.info("Creating new task: '{}'", request.title());
            return taskService.createTask(request);
    }
    

    @Operation(summary = "Retrieve all tasks (Paginated)")
    @GetMapping
    @Timed(value = "api.tasks.list", description = "Time taken to list tasks", histogram = true)
    public Page<TaskResponse> getAllTasks(
            @Parameter(description = "Pagination (page, size, sort)")
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable
    ) {
        return taskService.getTasks(pageable);
    }


    @Operation(summary = "Retrieve a task")
    @GetMapping("/{id}")
    @Timed(value = "api.tasks.get", description = "Time taken to get a task")
    public TaskResponse getTask(@PathVariable String id) {
        return taskService.getTask(id);
    }

    @Operation(summary = "Update a task")
    @PutMapping("/{id}")
    @Timed(value = "api.tasks.update", description = "Time taken to update a task",histogram = true)
    public TaskResponse updateTask(@PathVariable String id, @Valid @RequestBody TaskRequest request) {
            return taskService.updateTask(id, request);
        }

    @Operation(summary = "Delete a task")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Timed(value = "api.tasks.delete", description = "Time taken to delete a task",histogram = true)
    public void deleteTask(@PathVariable String id) {
            log.info("Deleting task id: '{}'", id);
            taskService.deleteTask(id);

    }
}