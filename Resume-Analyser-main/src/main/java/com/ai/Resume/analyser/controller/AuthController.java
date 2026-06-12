package com.ai.Resume.analyser.controller;

import com.ai.Resume.analyser.model.UserLogin;
import com.ai.Resume.analyser.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("resumeAnalyser/entry/v1")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true", allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS})
public class AuthController {

    @Autowired
    private AuthService service;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        if (password == null || password.length() < 6 || password.length() > 20) {
            return ResponseEntity.badRequest().body("Password must be between 6 and 20 characters");
        }
        String username = email.split("@")[0];
        return ResponseEntity.ok(service.simpleRegister(email, password, username));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLogin req) {
        return service.login(req);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return service.logout();
    }
}
