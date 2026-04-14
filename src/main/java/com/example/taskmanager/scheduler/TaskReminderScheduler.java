package com.example.taskmanager.scheduler;

import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class TaskReminderScheduler {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmailService emailService;

    // Runs every day at 9:00 AM IST (3:30 AM UTC)
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void sendDueTomorrowReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // FIX: query only tasks due tomorrow that are not done — don't load all tasks
        List<Task> tasksDueTomorrow = taskRepository
                .findByDueDateAndStatusNot(tomorrow, Task.Status.DONE);

        for (Task task : tasksDueTomorrow) {
            User user = task.getUser();
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                emailService.sendTaskReminder(
                        user.getEmail(),
                        user.getUsername(),
                        task.getTitle(),
                        task.getDueDate().toString()
                );
            }
        }

        System.out.println("Reminder job ran — sent " + tasksDueTomorrow.size() + " reminder(s) for " + tomorrow);
    }
}