package com.taskhub.taskservice.service.impl;

import com.taskhub.taskservice.domain.Task;
import com.taskhub.taskservice.dto.TaskRequest;
import com.taskhub.taskservice.dto.TaskResponse;
import com.taskhub.taskservice.exception.TaskNotFoundException;
import com.taskhub.taskservice.repository.TaskRepository;
import com.taskhub.taskservice.service.TaskService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
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

    @Override
    public List<TaskResponse> getTasks() {
        return repository
                .findAll()
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Override
    public void deleteTask(String id) {
        Task task = repository.findById(id).orElseThrow(TaskNotFoundException::new);
        repository.delete(task);

    }

    @Override
    public TaskResponse getTask(String id) {
        Task task =  repository.findById(id)
                .orElseThrow(TaskNotFoundException::new);
          return TaskResponse.from(task); // TODO user Mapstruct after MVP
    }

    @Override
    public TaskResponse updateTask(String id, TaskRequest request) {
        Task task = repository.findById(id).orElseThrow(TaskNotFoundException::new);
        task.update(request.title(),request.dueDate());
        return TaskResponse.from(task);
    }


}