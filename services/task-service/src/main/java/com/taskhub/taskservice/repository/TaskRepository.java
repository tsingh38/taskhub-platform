package com.taskhub.taskservice.repository;

import com.taskhub.taskservice.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, String> {
}