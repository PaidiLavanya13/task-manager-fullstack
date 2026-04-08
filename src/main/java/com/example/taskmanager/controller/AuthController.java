package com.example.taskmanager.controller;

import com.example.taskmanager.config.JwtUtil;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JwtUtil jwtUtil;
    @GetMapping("/test")
public ResponseEntity<?> test() {
    return ResponseEntity.ok(Map.of("status", "backend is alive"));
}

    @Autowired
    private PasswordEncoder passwordEncoder; // FIX: was missing — needed to hash passwords

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        // FIX: check if username already taken
        if (userRepo.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username already exists"));
        }

        // FIX: hash the password before saving — never store plain text
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        User existing = userRepo.findByUsername(user.getUsername())
                .orElse(null);

        // FIX: use constant-time BCrypt check instead of .equals() on plain text
        if (existing == null || !passwordEncoder.matches(user.getPassword(), existing.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }

        String token = jwtUtil.generateToken(existing.getUsername());

        // FIX: return both token AND userId so frontend can filter tasks by user
        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", existing.getId(),
                "username", existing.getUsername()
        ));
    }
}