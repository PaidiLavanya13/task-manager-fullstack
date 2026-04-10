package com.example.taskmanager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.reminder.sender}")
    private String senderEmail;

    // ── Task reminder email ────────────────────────────────────────────────
    public void sendTaskReminder(String toEmail, String username,
                                  String taskTitle, String dueDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject("⏰ Task Reminder: " + taskTitle + " is due tomorrow!");
            helper.setText(buildReminderHtml(username, taskTitle, dueDate), true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send reminder email to " + toEmail + ": " + e.getMessage());
        }
    }

    // ── Password reset email ───────────────────────────────────────────────
    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔐 TaskFlow — Reset Your Password");
            helper.setText(buildResetHtml(username, resetLink), true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send reset email to " + toEmail + ": " + e.getMessage());
        }
    }

    // ── HTML templates ─────────────────────────────────────────────────────
    private String buildReminderHtml(String username, String taskTitle, String dueDate) {
        return """
            <div style="font-family: 'Segoe UI', sans-serif; max-width: 500px; margin: auto;
                        background: #f8fafc; border-radius: 12px; padding: 32px;">
              <h2 style="color: #6366f1;">⏰ Task Reminder</h2>
              <p>Hi <strong>%s</strong>,</p>
              <p>Your task is due <strong>tomorrow</strong>:</p>
              <div style="background: white; border-left: 4px solid #6366f1;
                          padding: 16px; border-radius: 8px; margin: 16px 0;">
                <strong style="font-size: 16px;">%s</strong><br/>
                <span style="color: #9ca3af;">Due: %s</span>
              </div>
              <p>Log in to <a href="https://task-manager-fullstack-gray-six.vercel.app"
                 style="color: #6366f1;">TaskFlow</a> to mark it complete.</p>
            </div>
            """.formatted(username, taskTitle, dueDate);
    }

    private String buildResetHtml(String username, String resetLink) {
        return """
            <div style="font-family: 'Segoe UI', sans-serif; max-width: 500px; margin: auto;
                        background: #f8fafc; border-radius: 12px; padding: 32px;">
              <h2 style="color: #6366f1;">🔐 Reset Your Password</h2>
              <p>Hi <strong>%s</strong>,</p>
              <p>We received a request to reset your TaskFlow password.
                 Click the button below to set a new password:</p>
              <div style="text-align: center; margin: 24px 0;">
                <a href="%s"
                   style="background: #6366f1; color: white; padding: 12px 28px;
                          border-radius: 8px; text-decoration: none; font-weight: 600;
                          font-size: 15px;">
                  Reset Password
                </a>
              </div>
              <p style="color: #9ca3af; font-size: 13px;">
                This link expires in <strong>1 hour</strong>.<br/>
                If you didn't request this, you can safely ignore this email.
              </p>
            </div>
            """.formatted(username, resetLink);
    }
}