package com.example.taskmanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${app.reminder.sender}")
    private String senderEmail;

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private void sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        try {
            String body = """
                {
                  "sender": { "name": "TaskFlow", "email": "%s" },
                  "to": [{ "email": "%s", "name": "%s" }],
                  "subject": "%s",
                  "htmlContent": %s
                }
                """.formatted(
                    senderEmail,
                    toEmail,
                    toName,
                    subject,
                    toJson(htmlContent)
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_URL))
                    .header("Content-Type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                System.out.println("==> Email sent successfully to: " + toEmail);
            } else {
                System.err.println("==> EMAIL FAILED [" + response.statusCode() + "]: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("==> EMAIL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendTaskReminder(String toEmail, String username,
                                  String taskTitle, String dueDate) {
        System.out.println("==> Sending reminder email to: " + toEmail);
        sendEmail(
            toEmail,
            username,
            "Task Reminder: " + taskTitle + " is due tomorrow!",
            buildReminderHtml(username, taskTitle, dueDate)
        );
    }

    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        System.out.println("==> Sending reset email to: " + toEmail);
        sendEmail(
            toEmail,
            username,
            "TaskFlow - Reset Your Password",
            buildResetHtml(username, resetLink)
        );
    }

    // Escapes HTML content to a valid JSON string value
    private String toJson(String html) {
        return "\"" + html
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                + "\"";
    }

    private String buildReminderHtml(String username, String taskTitle, String dueDate) {
        return "<div style=\"font-family: 'Segoe UI', sans-serif; max-width: 500px; margin: auto; background: #f8fafc; border-radius: 12px; padding: 32px;\">"
             + "<h2 style=\"color: #6366f1;\">Task Reminder</h2>"
             + "<p>Hi <strong>" + username + "</strong>,</p>"
             + "<p>Your task is due <strong>tomorrow</strong>:</p>"
             + "<div style=\"background: white; border-left: 4px solid #6366f1; padding: 16px; border-radius: 8px; margin: 16px 0;\">"
             + "<strong style=\"font-size: 16px;\">" + taskTitle + "</strong><br/>"
             + "<span style=\"color: #9ca3af;\">Due: " + dueDate + "</span>"
             + "</div>"
             + "<p>Log in to <a href=\"https://task-manager-fullstack-gray-six.vercel.app\" style=\"color: #6366f1;\">TaskFlow</a> to mark it complete.</p>"
             + "</div>";
    }

    private String buildResetHtml(String username, String resetLink) {
        return "<div style=\"font-family: 'Segoe UI', sans-serif; max-width: 500px; margin: auto; background: #f8fafc; border-radius: 12px; padding: 32px;\">"
             + "<h2 style=\"color: #6366f1;\">Reset Your Password</h2>"
             + "<p>Hi <strong>" + username + "</strong>,</p>"
             + "<p>We received a request to reset your TaskFlow password. Click the button below:</p>"
             + "<div style=\"text-align: center; margin: 24px 0;\">"
             + "<a href=\"" + resetLink + "\" style=\"background: #6366f1; color: white; padding: 12px 28px; border-radius: 8px; text-decoration: none; font-weight: 600;\">Reset Password</a>"
             + "</div>"
             + "<p style=\"color: #9ca3af; font-size: 13px;\">This link expires in <strong>1 hour</strong>.<br/>If you did not request this, ignore this email.</p>"
             + "</div>";
    }
}