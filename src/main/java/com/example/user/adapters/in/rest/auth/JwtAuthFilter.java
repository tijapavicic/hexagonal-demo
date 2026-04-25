package com.example.user.adapters.in.rest.auth;

import com.example.config.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;

import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Inbound security adapter: extracts a Bearer JWT from the {@code Authorization}
 * header, validates it, and populates the Spring Security context.
 *
 * <p><strong>Note:</strong> This filter is no longer registered in the Spring Security
 * filter chain. Token validation is now handled by the composite {@link
 * com.example.config.SecurityConfig#compositeJwtDecoder()} which validates both
 * custom HMAC tokens and Keycloak RS256 tokens via the OAuth2 resource server DSL.
 * This class is retained for reference and potential future use.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            if (jwtUtil.isValid(token)) {
                String username = jwtUtil.extractUsername(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}

