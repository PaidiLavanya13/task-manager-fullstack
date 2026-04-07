package com.example.taskmanager.controller;

import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository repo;
    private final UserRepository userRepository;

    public TaskController(TaskRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    // FIX: Get userId from the JWT via @AuthenticationPrincipal, NOT from a request param.
    // Before: any logged-in user could pass ?userId=1 and read someone else's tasks.
    // Now: the user can only ever see their own tasks.
    private Long getAuthenticatedUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @GetMapping
    public List<Task> getTasks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority
    ) {
        Long userId = getAuthenticatedUserId(userDetails);
        List<Task> tasks = repo.findByUserId(userId);

        if (status != null && !status.isBlank()) {
            tasks = tasks.stream()
                    .filter(t -> t.getStatus().name().equalsIgnoreCase(status))
                    .toList();
        }

        if (priority != null && !priority.isBlank()) {
            tasks = tasks.stream()
                    .filter(t -> t.getPriority().name().equalsIgnoreCase(priority))
                    .toList();
        }

        return tasks;
    }

    @GetMapping("/search")
    public List<Task> searchTasks(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String keyword
    ) {
        Long userId = getAuthenticatedUserId(userDetails);
        return repo.findByUserIdAndTitleContainingIgnoreCase(userId, keyword);
    }

    @PostMapping
    public ResponseEntity<Task> createTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Task task
    ) {
        // FIX: Automatically assign the task to the authenticated user.
        // Before: the frontend had to pass userId in the body — easily spoofed.
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        task.setUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Task updatedTask
    ) {
        Long userId = getAuthenticatedUserId(userDetails);

        // FIX: verify the task belongs to the authenticated user before updating.
        // Before: any user could update any task by guessing an id.
        Task task = repo.findById(id)
                .filter(t -> t.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found or access denied"));
        // FIX: was throwing raw RuntimeException → exposed stack trace in 500 response.
        // Now throws ResponseStatusException → clean 404 JSON response.

        task.setTitle(updatedTask.getTitle());
        task.setDescription(updatedTask.getDescription());
        task.setCompleted(updatedTask.isCompleted());
        task.setStatus(updatedTask.getStatus());
        task.setPriority(updatedTask.getPriority());
        task.setCategory(updatedTask.getCategory());
        task.setDueDate(updatedTask.getDueDate());

        return ResponseEntity.ok(repo.save(task));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        Long userId = getAuthenticatedUserId(userDetails);

        // FIX: verify ownership before deleting — same as updateTask above.
        Task task = repo.findById(id)
                .filter(t -> t.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found or access denied"));

        repo.delete(task);
        return ResponseEntity.noContent().build();
    }
}