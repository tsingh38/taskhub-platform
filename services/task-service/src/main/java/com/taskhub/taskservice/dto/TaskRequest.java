package com.taskhub.taskservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record TaskRequest(
        @NotBlank(message = "Title cannot be empty")
        String title,
        @Future(message = "Due date must be in the future")
        LocalDateTime dueDate
) {}