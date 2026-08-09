package org.brian.aisupportagent.service;

import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.auth.LoginRequest;
import org.brian.aisupportagent.dto.auth.RegisterRequest;
import org.brian.aisupportagent.dto.auth.AuthenticationResponse;
import org.brian.aisupportagent.entity.Role;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.exception.EmailAlreadyExistsException;
import org.brian.aisupportagent.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // 1. REGISTER FLOW
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        // Check if email already exists
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        // Encrypt password & build user entity
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password())) // BCrypt hashing
                .role(Role.EMPLOYEE)
                .build();

        // Save user to database
        userRepository.save(user);

        // Generate Access and Refresh tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 2. LOGIN FLOW
    public AuthenticationResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, request.password())
        );
        User user = (User) authentication.getPrincipal();

        // Generate tokens upon successful validation
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 3. REFRESH TOKEN FLOW
    @Transactional
    public AuthenticationResponse refreshToken(String refreshToken) {
        RefreshTokenRotation rotation = refreshTokenService.rotateRefreshToken(refreshToken);
        String newAccessToken = jwtService.generateToken(rotation.user());

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(rotation.refreshToken())
                .build();
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }
}
