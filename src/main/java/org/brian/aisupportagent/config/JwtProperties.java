package org.brian.aisupportagent.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenExpiration
) {
    public JwtProperties {
        if (accessTokenExpiration != null
                && (accessTokenExpiration.isZero() || accessTokenExpiration.isNegative())) {
            throw new IllegalArgumentException("JWT access-token expiration must be positive");
        }
    }
}
