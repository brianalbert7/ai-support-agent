package org.brian.aisupportagent.dto.auth;

public record LoginRequest(
        String email,
        String password
) {}