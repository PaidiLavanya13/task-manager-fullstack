package com.example.taskmanager.scheduler;

import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import com.example.taskmanager.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class TaskReminderScheduler {

    @Autowired
    private TaskRepository taskRepository;

    // @Autowired
    // private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    // Runs every day at 9:00 AM
    @Scheduled(cron = "0 40 17 * * *", zone = "Asia/Kolkata")
    public void sendDueTomorrowReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Task> tasksDueTomorrow = taskRepository.findAll().stream()
            .filter(t -> tomorrow.equals(t.getDueDate()))
            .filter(t -> t.getStatus() != Task.Status.DONE)
            .toList();

        for (Task task : tasksDueTomorrow) {
            User user = task.getUser();

if (user != null && user.getEmail() != null) {
    emailService.sendTaskReminder(
        user.getEmail(),
        user.getUsername(),
        task.getTitle(),
        task.getDueDate().toString()
    );
}
        }
    }
}