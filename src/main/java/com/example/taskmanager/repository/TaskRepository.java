package com.example.taskmanager.repository;

import com.example.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // Get all tasks for a user
    List<Task> findByUserId(Long userId);

    // FIX: Added search method used by TaskController's /search endpoint
    List<Task> findByUserIdAndTitleContainingIgnoreCase(Long userId, String keyword);
}