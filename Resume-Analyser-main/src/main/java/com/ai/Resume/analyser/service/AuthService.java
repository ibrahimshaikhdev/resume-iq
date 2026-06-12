package com.ai.Resume.analyser.service;

import com.ai.Resume.analyser.exception.EmailAlreadyExistsException;
import com.ai.Resume.analyser.exception.InvalidCredentialsException;
import com.ai.Resume.analyser.jwt.JwtService;
import com.ai.Resume.analyser.model.*;
import com.ai.Resume.analyser.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Autowired
    private JwtService jwt;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private UserRepository userRepository;

    public String simpleRegister(String email, String password, String username) {
        String normalizedEmail = email.toLowerCase().trim();

        if (userRepository.existsById(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email already registered. Please sign in.");
        }

        User newUser = User.builder()
                .username(username)
                .email(normalizedEmail)
                .password(passwordEncoder.encode(password))
                .previousResults(false)
                .build();
        userRepository.save(newUser);
        return "Account created successfully";
    }

    public ResponseEntity<LoginResponse> login(@Valid UserLogin req) {
        String normalizedEmail = req.getEmail().toLowerCase().trim();

        try {
            authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, req.getPassword()));
            String token = jwt.generateToken(normalizedEmail);
            User user = userRepository.findById(normalizedEmail).orElse(null);
            HttpHeaders headers = new HttpHeaders();
            ResponseCookie cookie = ResponseCookie.from("entrypasstoken", token).path("/").httpOnly(true).maxAge(20 * 24 * 60 * 60).sameSite("Strict").secure(false).build();
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
            LoginResponse loginRes = new LoginResponse(user.getUsername(), user.getPreviousResults());
            return new ResponseEntity<>(loginRes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    public ResponseEntity<String> logout() {
        ResponseCookie cookie = ResponseCookie.from("entrypasstoken", "").path("/").httpOnly(true).maxAge(0).sameSite("Strict").build();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return new ResponseEntity<>("Logged out successfully", headers, org.springframework.http.HttpStatus.OK);
    }

    public ResponseEntity<String> deleteAccount() {
        // This is handled in ResumeController
        return ResponseEntity.ok("Account deleted");
    }
}
