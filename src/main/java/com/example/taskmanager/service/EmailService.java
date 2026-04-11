package com.example.taskmanager.service;

import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${app.reminder.sender}")
    private String senderEmail;

    public void sendTaskReminder(String toEmail, String username,
                                  String taskTitle, String dueDate) {
        try {
            Resend resend = new Resend(resendApiKey);
            SendEmailRequest request = CreateEmailOptions.builder()
                .from(senderEmail)
                .to(toEmail)
                .subject("⏰ Task Reminder: " + taskTitle + " is due tomorrow!")
                .html(buildReminderHtml(username, taskTitle, dueDate))
                .build();
            resend.emails().send(request);
        } catch (Exception e) {
            System.err.println("Failed to send reminder email to " + toEmail + ": " + e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        try {
            Resend resend = new Resend(resendApiKey);
            SendEmailRequest request = CreateEmailOptions.builder()
                .from(senderEmail)
                .to(toEmail)
                .subject("🔐 TaskFlow — Reset Your Password")
                .html(buildResetHtml(username, resetLink))
                .build();
            resend.emails().send(request);
        } catch (Exception e) {
            System.err.println("Failed to send reset email to " + toEmail + ": " + e.getMessage());
        }
    }

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
                          border-radius: 8px; text-decoration: none; font-weight: 600;">
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