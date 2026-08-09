package org.brian.aisupportagent.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.service.JwtService;
import jakarta.annotation.Nonnull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService; // This injects your CustomUserDetailsService

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Read Authorization header. If it's missing or doesn't start with "Bearer ", skip it.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the literal token (skipping the "Bearer " prefix which is 7 characters long)
        jwt = authHeader.substring(7);
        userEmail = jwtService.extractUsername(jwt);

        // 3. If there is an email and the user is not already authenticated in this request session
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 4. Load the user details from the database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 5. Validate the JWT token against the database user details
            // NOTE: If your JwtService expects your custom User entity, you can cast userDetails or update your JwtService signature
            if (jwtService.isTokenValid(jwt, (org.brian.aisupportagent.entity.User) userDetails)) {

                // 6. Build the Spring Security authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Enforce details from the web request inside the token profile
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Update Spring Security Context with the authorized user token
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 8. Continue down the chain to allow the Controller to execute
        filterChain.doFilter(request, response);
    }
}
