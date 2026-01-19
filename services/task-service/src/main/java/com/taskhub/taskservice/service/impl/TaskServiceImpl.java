package com.taskhub.taskservice.service.impl;

import com.taskhub.taskservice.domain.Task;
import com.taskhub.taskservice.dto.TaskRequest;
import com.taskhub.taskservice.dto.TaskResponse;
import com.taskhub.taskservice.repository.TaskRepository;
import com.taskhub.taskservice.service.TaskService;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository repository;

    public TaskServiceImpl(TaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public TaskResponse createTask(TaskRequest request) {
        Task task = new Task(request.title(), request.dueDate());
        Task saved = repository.save(task);
        return TaskResponse.from(saved);
    }
}