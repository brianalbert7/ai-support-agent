package org.brian.aisupportagent.service;

import org.brian.aisupportagent.dto.auth.AuthenticationResponse;
import org.brian.aisupportagent.dto.auth.LoginRequest;
import org.brian.aisupportagent.dto.auth.RegisterRequest;
import org.brian.aisupportagent.entity.Role;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.exception.EmailAlreadyExistsException;
import org.brian.aisupportagent.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void registersEmployeeWithNormalizedEmailAndHashedPassword() {
        RegisterRequest request = new RegisterRequest(
                "Brian",
                "Albert",
                "  Brian@Example.COM  ",
                "plain-password"
        );
        when(userRepository.existsByEmail("brian@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
        when(jwtService.generateToken(org.mockito.ArgumentMatchers.any(User.class)))
                .thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(org.mockito.ArgumentMatchers.any(User.class)))
                .thenReturn("refresh-token");

        AuthenticationResponse response = authenticationService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("brian@example.com", savedUser.getEmail());
        assertEquals("hashed-password", savedUser.getPassword());
        assertEquals(Role.EMPLOYEE, savedUser.getRole());
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
    }

    @Test
    void rejectsRegistrationWhenNormalizedEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "Brian",
                "Albert",
                "  Brian@Example.COM  ",
                "plain-password"
        );
        when(userRepository.existsByEmail("brian@example.com")).thenReturn(true);

        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authenticationService.register(request)
        );

        assertTrue(exception.getMessage().contains("brian@example.com"));
        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void returnsRotatedRefreshTokenAndNewAccessToken() {
        User user = User.builder()
                .email("employee@example.com")
                .role(Role.EMPLOYEE)
                .build();
        when(refreshTokenService.rotateRefreshToken("old-refresh-token"))
                .thenReturn(new RefreshTokenRotation(user, "new-refresh-token"));
        when(jwtService.generateToken(user)).thenReturn("new-access-token");

        AuthenticationResponse response = authenticationService.refreshToken("old-refresh-token");

        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
    }

    @Test
    void authenticatesNormalizedCredentialsBeforeIssuingTokens() {
        LoginRequest request = new LoginRequest(
                "  Employee@Example.COM  ",
                "plain-password"
        );
        User user = User.builder()
                .email("employee@example.com")
                .role(Role.EMPLOYEE)
                .build();
        Authentication authenticatedUser = UsernamePasswordAuthenticationToken.authenticated(
                user,
                null,
                user.getAuthorities()
        );
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(Authentication.class)))
                .thenReturn(authenticatedUser);
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        AuthenticationResponse response = authenticationService.login(request);

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        Authentication authenticationRequest = authenticationCaptor.getValue();

        assertEquals("employee@example.com", authenticationRequest.getPrincipal());
        assertEquals("plain-password", authenticationRequest.getCredentials());
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
    }

    @Test
    void doesNotIssueTokensWhenCredentialsAreRejected() {
        LoginRequest request = new LoginRequest("employee@example.com", "wrong-password");
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authenticationService.login(request));

        verifyNoInteractions(jwtService, refreshTokenService);
    }
}
