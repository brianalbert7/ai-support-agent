package org.brian.aisupportagent.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be 255 characters or fewer")
        String email,

        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Password must be 72 characters or fewer")
        String password
) {}
