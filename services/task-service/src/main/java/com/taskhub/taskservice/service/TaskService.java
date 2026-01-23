package com.taskhub.taskservice.service;

import com.taskhub.taskservice.dto.TaskRequest;
import com.taskhub.taskservice.dto.TaskResponse;

import java.util.List;

public interface TaskService {

    TaskResponse createTask(TaskRequest request);

    List<TaskResponse> getTasks();

    void deleteTask(String id);

    TaskResponse getTask(String id);

    TaskResponse updateTask(String id, TaskRequest request);
}