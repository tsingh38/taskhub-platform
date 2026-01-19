package com.taskhub.taskservice.dto;

public class TaskRequest {

    private String title;
    private String description;

    public TaskRequest() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}