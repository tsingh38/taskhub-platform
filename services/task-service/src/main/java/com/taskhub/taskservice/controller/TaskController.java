package com.taskhub.taskservice.controller;

import com.taskhub.taskservice.dto.TaskResponse;
import com.taskhub.taskservice.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/health")
    public TaskResponse health() {
        return taskService.health();
    }
}