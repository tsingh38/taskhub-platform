package com.taskhub.taskservice.domain;

import java.time.Instant;
import java.util.UUID;

public class Task {

    private final UUID id;
    private final String title;
    private final String description;
    private final Instant createdAt;

    public Task(UUID id, String title, String description, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}