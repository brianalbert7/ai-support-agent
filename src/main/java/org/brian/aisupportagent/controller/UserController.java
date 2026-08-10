package org.brian.aisupportagent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.brian.aisupportagent.dto.UserResponse;
import org.brian.aisupportagent.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.brian.aisupportagent.config.OpenApiConfig.BEARER_AUTH_SCHEME;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Authenticated user profile")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
public class UserController {

    // GET /api/users/me
    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user profile")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User authenticatedUser) {

        UserResponse response = UserResponse.builder()
                .id(authenticatedUser.getId())
                .firstName(authenticatedUser.getFirstName())
                .lastName(authenticatedUser.getLastName())
                .email(authenticatedUser.getEmail())
                .role(authenticatedUser.getRole())
                .build();

        return ResponseEntity.ok(response);
    }
}
