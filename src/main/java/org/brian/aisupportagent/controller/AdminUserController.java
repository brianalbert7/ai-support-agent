package org.brian.aisupportagent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.UserResponse;
import org.brian.aisupportagent.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.brian.aisupportagent.config.OpenApiConfig.BEARER_AUTH_SCHEME;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Administrative user management")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List registered users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
