package org.brian.aisupportagent.dto;

import lombok.Builder;
import org.brian.aisupportagent.entity.Role;

import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Role role
) {}
