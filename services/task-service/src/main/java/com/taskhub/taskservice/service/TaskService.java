package com.taskhub.taskservice.service;

import com.taskhub.taskservice.dto.TaskRequest;
import com.taskhub.taskservice.dto.TaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskService {

    TaskResponse createTask(TaskRequest request);

    Page<TaskResponse> getTasks(Pageable pageable);

    void deleteTask(String id);

    TaskResponse getTask(String id);

    TaskResponse updateTask(String id, TaskRequest request);
}