package com.example.taskmanager.controller;

import com.example.taskmanager.entity.Task;
import com.example.taskmanager.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks") // FIX: was "/tasks" — use consistent "/api/" prefix
public class TaskController {

    private final TaskRepository repo; // FIX: renamed from taskRepository to match constructor

    public TaskController(TaskRepository repo) {
        this.repo = repo;
    }

    // GET all tasks for a user — optionally filter by status or priority
    @GetMapping
    public List<Task> getTasks(
            @RequestParam Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority
    ) {
        // FIX: was using "taskRepository" (undefined variable) — now uses "repo"
        List<Task> tasks = repo.findByUserId(userId);

        // Filter by status if provided
        if (status != null && !status.isBlank()) {
            tasks = tasks.stream()
                    .filter(t -> t.getStatus().name().equalsIgnoreCase(status))
                    .toList();
        }

        // Filter by priority if provided
        if (priority != null && !priority.isBlank()) {
            tasks = tasks.stream()
                    .filter(t -> t.getPriority().name().equalsIgnoreCase(priority))
                    .toList();
        }

        return tasks;
    }

    // Search tasks by keyword in title or description
    @GetMapping("/search")
    public List<Task> searchTasks(@RequestParam Long userId, @RequestParam String keyword) {
        return repo.findByUserIdAndTitleContainingIgnoreCase(userId, keyword);
    }

    // Create a new task
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(task));
    }

    // Update an existing task
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task updatedTask) {
        Task task = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        task.setTitle(updatedTask.getTitle());
        task.setDescription(updatedTask.getDescription());
        task.setCompleted(updatedTask.isCompleted());
        task.setStatus(updatedTask.getStatus());
        task.setPriority(updatedTask.getPriority());
        task.setCategory(updatedTask.getCategory());
        task.setDueDate(updatedTask.getDueDate());  // FIX: was not updating new fields

        return ResponseEntity.ok(repo.save(task));
    }

    // Delete a task
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build(); // FIX: was silently succeeding even for missing IDs
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}