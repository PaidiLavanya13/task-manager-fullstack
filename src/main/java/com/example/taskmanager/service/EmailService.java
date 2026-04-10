package com.example.taskmanager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.reminder.sender}")
    private String senderEmail;

    public void sendTaskReminder(String toEmail, String username, String taskTitle, String dueDate) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(toEmail);
        message.setSubject("⏰ Task Due Tomorrow: " + taskTitle);
        message.setText(
            "Hi " + username + ",\n\n" +
            "This is a reminder that your task is due tomorrow!\n\n" +
            "📌 Task: " + taskTitle + "\n" +
            "📅 Due Date: " + dueDate + "\n\n" +
            "Log in to complete it: https://taskmanager-bay-psi.vercel.app\n\n" +
            "Good luck!\n" +
            "Task Manager"
        );
        mailSender.send(message);
    }
}