package org.brian.aisupportagent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    // --- UserDetails Required Methods ---

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        // Maps your string role (e.g., "USER", "ADMIN") to a Spring Security authority
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public @NonNull String getUsername() {
        return email; // Uses email as the login credential
    }

}
