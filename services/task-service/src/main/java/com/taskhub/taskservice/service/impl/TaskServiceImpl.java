package com.taskhub.taskservice.service.impl;

import com.taskhub.taskservice.dto.TaskResponse;
import com.taskhub.taskservice.service.TaskService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TaskServiceImpl implements TaskService {

    @Override
    public TaskResponse health() {
        return new TaskResponse(
                UUID.randomUUID(),
                "Task Service",
                "Service is running",
                Instant.now()
        );
    }
}