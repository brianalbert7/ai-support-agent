package org.brian.aisupportagent.service;

import org.brian.aisupportagent.entity.RefreshToken;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.exception.InvalidRefreshTokenException;
import org.brian.aisupportagent.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void createsRandomTokenButStoresOnlyItsHash() {
        User user = User.builder().email("employee@example.com").build();

        String rawToken = refreshTokenService.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository).flush();
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken storedToken = tokenCaptor.getValue();
        assertEquals(hash(rawToken), storedToken.getTokenHash());
        assertNotEquals(rawToken, storedToken.getTokenHash());
        assertEquals(64, storedToken.getTokenHash().length());
        assertEquals(user, storedToken.getUser());
        assertTrue(storedToken.getExpiration().isAfter(Instant.now()));
    }

    @Test
    void rotatesValidTokenAndDeletesOldToken() {
        String oldRawToken = "old-raw-token";
        User user = User.builder().email("employee@example.com").build();
        RefreshToken storedToken = RefreshToken.builder()
                .tokenHash(hash(oldRawToken))
                .expiration(Instant.now().plusSeconds(60))
                .user(user)
                .build();
        when(refreshTokenRepository.findByTokenHash(hash(oldRawToken)))
                .thenReturn(Optional.of(storedToken));

        RefreshTokenRotation rotation = refreshTokenService.rotateRefreshToken(oldRawToken);

        verify(refreshTokenRepository).delete(storedToken);
        verify(refreshTokenRepository).flush();
        assertEquals(user, rotation.user());
        assertNotEquals(oldRawToken, rotation.refreshToken());

        ArgumentCaptor<RefreshToken> newTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(newTokenCaptor.capture());
        assertEquals(hash(rotation.refreshToken()), newTokenCaptor.getValue().getTokenHash());
    }

    @Test
    void rejectsExpiredToken() {
        String rawToken = "expired-raw-token";
        RefreshToken storedToken = RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .expiration(Instant.now().minusSeconds(1))
                .user(User.builder().email("employee@example.com").build())
                .build();
        when(refreshTokenRepository.findByTokenHash(hash(rawToken)))
                .thenReturn(Optional.of(storedToken));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotateRefreshToken(rawToken)
        );

    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
