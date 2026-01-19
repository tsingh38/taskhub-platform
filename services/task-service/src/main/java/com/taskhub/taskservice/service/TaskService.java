package com.taskhub.taskservice.service;

import com.taskhub.taskservice.dto.TaskRequest;
import com.taskhub.taskservice.dto.TaskResponse;

public interface TaskService {
    TaskResponse createTask(TaskRequest request);
}