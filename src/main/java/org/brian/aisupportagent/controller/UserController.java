package org.brian.aisupportagent.controller;

import org.brian.aisupportagent.dto.UserResponse;
import org.brian.aisupportagent.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // GET /api/users/me
    @GetMapping("/me")
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
