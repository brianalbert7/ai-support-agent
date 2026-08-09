package org.brian.aisupportagent.dto.auth;

import lombok.Builder;
import org.brian.aisupportagent.entity.RefreshToken;

@Builder
public record AuthenticationResponse(
        String accessToken,
        String refreshToken
) {}
