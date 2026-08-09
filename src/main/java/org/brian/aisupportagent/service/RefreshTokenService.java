package org.brian.aisupportagent.service;

import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.entity.RefreshToken;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.exception.InvalidRefreshTokenException;
import org.brian.aisupportagent.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final long EXPIRATION_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    // 1. Create a secure, unique database-backed refresh token
    @Transactional
    public String createRefreshToken(User user) {

        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();
        return issueToken(user);
    }

    @Transactional
    public RefreshTokenRotation rotateRefreshToken(String rawToken) {
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!storedToken.getExpiration().isAfter(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        User user = storedToken.getUser();
        refreshTokenRepository.delete(storedToken);
        refreshTokenRepository.flush();

        String newRawToken = issueToken(user);
        return new RefreshTokenRotation(user, newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String issueToken(User user) {
        String rawToken = generateToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiration(Instant.now().plus(EXPIRATION_DAYS, ChronoUnit.DAYS))
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] tokenHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(tokenHash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
