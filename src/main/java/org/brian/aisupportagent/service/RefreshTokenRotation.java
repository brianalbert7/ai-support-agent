package org.brian.aisupportagent.service;

import org.brian.aisupportagent.entity.User;

public record RefreshTokenRotation(User user, String refreshToken) {}
