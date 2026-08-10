package org.brian.aisupportagent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.auth.AuthenticationResponse;
import org.brian.aisupportagent.dto.auth.LoginRequest;
import org.brian.aisupportagent.dto.auth.RefreshTokenRequest;
import org.brian.aisupportagent.dto.auth.RegisterRequest;
import org.brian.aisupportagent.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, authenticate, and manage tokens")
public class AuthController {

    private final AuthenticationService authService;

    // 1. POST /api/auth/register
    @PostMapping("/register")
    @Operation(summary = "Register a user")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthenticationResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. POST /api/auth/login
    @PostMapping("/login")
    @Operation(summary = "Log in with email and password")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // 3. POST /api/auth/refresh
    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token")
    public ResponseEntity<AuthenticationResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthenticationResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
