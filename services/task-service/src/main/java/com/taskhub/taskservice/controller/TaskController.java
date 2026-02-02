package com.taskhub.taskservice.controller;

import com.taskhub.taskservice.dto.TaskRequest;
import com.taskhub.taskservice.dto.TaskResponse;
import com.taskhub.taskservice.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@Tag(name = "Tasks", description = "Task management operations")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Create a task", description = "Creates a new task with a pending status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Task created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create( @Valid  @RequestBody TaskRequest request) {
        return taskService.createTask(request);
    }

    @Operation(summary = "Retrieve all the tasks", description = "Get all the tasks")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrieve all the existing tasks"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @GetMapping
    public List<TaskResponse> getAllTasks() {
        return taskService.getTasks();
    }

    @Operation(summary = "Retrieve the task", description = "Get a task with given id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task created successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable String id) {
        return taskService.getTask(id);
    }

    @Operation(summary = "Update the task", description = "Update the task with given id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task updated successfully"),
            @ApiResponse(responseCode = "400", description = "Task not found")
    })
    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @PathVariable String id,
            @Valid @RequestBody TaskRequest request
    ) {
        return taskService.updateTask(id, request);
    }

    @Operation(summary = "Delete the task", description = "Delete the task with given id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable String id) {
        taskService.deleteTask(id);
    }
}