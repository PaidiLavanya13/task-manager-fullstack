package com.example.taskmanager.controller;

import com.example.taskmanager.config.JwtUtil;
import com.example.taskmanager.entity.PasswordResetToken;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.PasswordResetTokenRepository;
import com.example.taskmanager.repository.UserRepository;
import com.example.taskmanager.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;
    @Autowired private PasswordResetTokenRepository tokenRepo;

    // Frontend URL for the reset link
    @Value("${APP_FRONTEND_URL}")
    private String frontendUrl;

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of("status", "backend is alive"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email    = body.get("email");

        if (username == null || password == null || email == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Username, password and email are required"));
        }
        if (userRepo.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username already exists"));
        }
        if (userRepo.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        userRepo.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        User existing = userRepo.findByUsername(username).orElse(null);
        if (existing == null || !passwordEncoder.matches(password, existing.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }

        String token = jwtUtil.generateToken(existing.getUsername());
        return ResponseEntity.ok(Map.of(
                "token",    token,
                "userId",   existing.getId(),
                "username", existing.getUsername()
        ));
    }

    // ── Forgot Password ────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        // Always return success to avoid email enumeration
        userRepo.findByEmail(email).ifPresent(user -> {
            // Delete any existing tokens for this user
            tokenRepo.deleteByUserId(user.getId());

            // Create new token valid for 1 hour
            String tokenValue = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(tokenValue);
            resetToken.setUser(user);
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
            tokenRepo.save(resetToken);

            // Send email with reset link
            String resetLink = frontendUrl + "/reset-password?token=" + tokenValue;
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetLink);
        });

        return ResponseEntity.ok(Map.of(
                "message", "If that email is registered, a reset link has been sent."
        ));
    }

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String tokenValue   = body.get("token");
        String newPassword  = body.get("password");

        if (tokenValue == null || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Token and password (min 6 chars) are required"));
        }

        PasswordResetToken resetToken = tokenRepo.findByToken(tokenValue).orElse(null);

        if (resetToken == null || resetToken.isUsed() || resetToken.isExpired()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Reset link is invalid or has expired. Please request a new one."));
        }

        // Update password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepo.save(resetToken);

        return ResponseEntity.ok(Map.of("message", "Password reset successful. You can now log in."));
    }
}