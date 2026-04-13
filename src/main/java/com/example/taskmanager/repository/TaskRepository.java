package com.example.taskmanager.repository;

import com.example.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserId(Long userId);

    List<Task> findByUserIdAndTitleContainingIgnoreCase(Long userId, String keyword);

    List<Task> findByDueDateAndStatusNot(LocalDate dueDate, Task.Status status);
}