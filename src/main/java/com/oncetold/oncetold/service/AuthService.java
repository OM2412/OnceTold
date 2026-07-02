package com.oncetold.oncetold.service;

import com.oncetold.oncetold.dto.AuthResponse;
import com.oncetold.oncetold.dto.LoginRequest;
import com.oncetold.oncetold.dto.RegisterRequest;
import com.oncetold.oncetold.entity.User;
import com.oncetold.oncetold.repository.UserRepository;
import com.oncetold.oncetold.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Register a new user account and return a JWT token.
     * Throws IllegalArgumentException if the email is already taken.
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        User saved = userRepository.save(user);
        return new AuthResponse(jwtUtil.generateToken(saved));
    }

    /**
     * Authenticate an existing user and return a JWT token.
     * Throws BadCredentialsException on invalid email or password.
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return new AuthResponse(jwtUtil.generateToken(user));
    }
}
