package com.taskhub.taskservice.service.impl;

import com.taskhub.taskservice.domain.Task;
import com.taskhub.taskservice.dto.TaskRequest;
import com.taskhub.taskservice.dto.TaskResponse;
import com.taskhub.taskservice.service.TaskService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskServiceImpl implements TaskService {

    private final Map<String, Task> store = new ConcurrentHashMap<>();

    @Override
    public TaskResponse createTask(TaskRequest request) {
        Task task = new Task(request.title(), request.dueDate());
        store.put(task.getId(), task);
        return TaskResponse.from(task);
    }
}