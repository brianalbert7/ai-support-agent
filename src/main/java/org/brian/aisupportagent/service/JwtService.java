package org.brian.aisupportagent.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.brian.aisupportagent.config.JwtProperties;
import org.brian.aisupportagent.entity.User;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = createSigningKey(properties.secret());
    }

    // 1. Generate Token using User entity data
    public String generateToken(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenExpiration());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    // 2. Extract Username (Email) from the token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 3. Validate if the token belongs to the user and is not expired
    public boolean isTokenValid(String token, User user) {
        Claims claims = extractAllClaims(token);
        return user.getEmail().equals(claims.getSubject())
                && ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey createSigningKey(String encodedSecret) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(encodedSecret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "JWT secret must be valid Base64 containing at least 32 random bytes",
                    exception
            );
        }
    }
}
