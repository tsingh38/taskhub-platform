package com.taskhub.taskservice.domain;

import com.taskhub.taskservice.service.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class Task {

    private final String id;
    private final String title;
    private final LocalDateTime dueDate;
    private TaskStatus status;

    public Task(String title, LocalDateTime dueDate) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title must not be blank");
        }
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Due date cannot be in the past");
        }

        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.dueDate = dueDate;
        this.status = TaskStatus.OPEN;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public LocalDateTime getDueDate() { return dueDate; }
    public TaskStatus getStatus() { return status; }

    public void markDone() {
        this.status = TaskStatus.DONE;
    }
}