package com.taskhub.taskservice.dto;

import com.taskhub.taskservice.domain.Task;
import com.taskhub.taskservice.service.TaskStatus;

import java.time.LocalDateTime;

public record TaskResponse(
        String id,
        String title,
        LocalDateTime dueDate,
       TaskStatus status
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDueDate(),
                task.getStatus()
        );
    }
}