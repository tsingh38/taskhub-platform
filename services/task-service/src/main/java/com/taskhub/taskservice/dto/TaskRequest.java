package com.taskhub.taskservice.dto;

import java.time.LocalDateTime;

public record TaskRequest(
        String title,
        LocalDateTime dueDate
) {}