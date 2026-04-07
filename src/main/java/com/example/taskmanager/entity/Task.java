package com.example.taskmanager.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    private boolean completed = false;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    private Category category = Category.OTHER;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private Status status = Status.TODO;

    public enum Priority { HIGH, MEDIUM, LOW }
    public enum Category { WORK, PERSONAL, HEALTH, SHOPPING, OTHER }
    public enum Status { TODO, IN_PROGRESS, DONE }
}