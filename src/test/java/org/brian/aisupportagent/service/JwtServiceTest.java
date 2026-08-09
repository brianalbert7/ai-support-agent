package org.brian.aisupportagent.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import org.brian.aisupportagent.config.JwtProperties;
import org.brian.aisupportagent.entity.Role;
import org.brian.aisupportagent.entity.User;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)
    );
    private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");

    @Test
    void generatesValidAccessTokenWithRequiredClaims() {
        JwtProperties properties = properties("ai-support-agent");
        JwtService jwtService = service(properties, NOW);
        User user = user("employee@example.com");

        String token = jwtService.generateToken(user);
        Claims claims = jwtService.extractClaim(token, claimsValue -> claimsValue);

        assertTrue(jwtService.isTokenValid(token, user));
        assertEquals("employee@example.com", claims.getSubject());
        assertEquals("ai-support-agent", claims.getIssuer());
        assertEquals("EMPLOYEE", claims.get("role", String.class));
        assertEquals("access", claims.get("token_type", String.class));
        assertEquals(NOW, claims.getIssuedAt().toInstant());
        assertEquals(NOW.plus(Duration.ofMinutes(15)), claims.getExpiration().toInstant());
        assertNotNull(claims.getId());
    }

    @Test
    void rejectsTokenForDifferentUser() {
        JwtService jwtService = service(properties("ai-support-agent"), NOW);
        String token = jwtService.generateToken(user("employee@example.com"));

        assertFalse(jwtService.isTokenValid(token, user("other@example.com")));
    }

    @Test
    void rejectsExpiredToken() {
        JwtProperties properties = properties("ai-support-agent");
        String token = service(properties, NOW).generateToken(user("employee@example.com"));
        JwtService laterService = service(properties, NOW.plus(Duration.ofMinutes(16)));

        assertThrows(ExpiredJwtException.class, () -> laterService.extractUsername(token));
    }

    @Test
    void rejectsTokenFromDifferentIssuer() {
        String token = service(properties("different-issuer"), NOW)
                .generateToken(user("employee@example.com"));
        JwtService expectedIssuerService = service(properties("ai-support-agent"), NOW);

        assertThrows(IncorrectClaimException.class, () -> expectedIssuerService.extractUsername(token));
    }

    @Test
    void rejectsWeakSigningKeyDuringConstruction() {
        String weakSecret = Base64.getEncoder().encodeToString(
                "too-short".getBytes(StandardCharsets.UTF_8)
        );
        JwtProperties properties = new JwtProperties(
                weakSecret,
                "ai-support-agent",
                Duration.ofMinutes(15)
        );

        assertThrows(
                IllegalStateException.class,
                () -> new JwtService(properties, Clock.fixed(NOW, ZoneOffset.UTC))
        );
    }

    private JwtService service(JwtProperties properties, Instant now) {
        return new JwtService(properties, Clock.fixed(now, ZoneOffset.UTC));
    }

    private JwtProperties properties(String issuer) {
        return new JwtProperties(SECRET, issuer, Duration.ofMinutes(15));
    }

    private User user(String email) {
        return User.builder()
                .email(email)
                .role(Role.EMPLOYEE)
                .build();
    }
}
