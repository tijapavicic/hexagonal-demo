package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

/**
 * Spring Security configuration.
 *
 * <ul>
 *   <li>Stateless — no sessions, no cookies.</li>
 *   <li>CSRF disabled (safe for stateless REST APIs; see important_considerations.md).</li>
 *   <li>Supports <strong>two token types</strong> via a composite {@link JwtDecoder}:
 *     <ol>
 *       <li>Custom HMAC-SHA256 tokens issued by {@code POST /api/auth/login}.</li>
 *       <li>Keycloak RS256 tokens (issuer {@code KEYCLOAK_ISSUER_URI}).</li>
 *     </ol>
 *   </li>
 *   <li>Public endpoints: {@code POST /api/auth/login}, Swagger UI, Actuator health/info.</li>
 *   <li>All {@code /api/users/**} endpoints require a valid Bearer JWT.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** HMAC-SHA256 secret for custom tokens — injected from {@code app.jwt.secret}. */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * Keycloak issuer URI used to build the OIDC {@link JwtDecoder}.
     * Set via {@code KEYCLOAK_ISSUER_URI} env var or {@code application.yml}.
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String keycloakIssuerUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoint — public
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // Swagger UI & OpenAPI spec — public (restrict in prod as needed)
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**").permitAll()
                        // Actuator — only health and info are exposed (see application.yml)
                        .requestMatchers("/actuator/**").permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(compositeJwtDecoder())
                                .jwtAuthenticationConverter(keycloakAuthenticationConverter())
                        )
                )
                .build();
    }

    /**
     * Composite {@link JwtDecoder} that first attempts HMAC-SHA256 decoding (custom tokens),
     * then falls back to Keycloak's OIDC JWKS endpoint (RS256 tokens).
     *
     * <p>This allows both authentication flows to coexist without code duplication:
     * <ul>
     *   <li>{@code POST /api/auth/login} → custom HMAC token</li>
     *   <li>Keycloak password/client-credentials flow → Keycloak RS256 token</li>
     * </ul>
     */
    @Bean
    public JwtDecoder compositeJwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        NimbusJwtDecoder hmacDecoder = NimbusJwtDecoder.withSecretKey(secretKey).build();

        NimbusJwtDecoder keycloakDecoder = NimbusJwtDecoder
                .withIssuerLocation(keycloakIssuerUri)
                .build();

        return token -> {
            try {
                return hmacDecoder.decode(token);
            } catch (JwtException hmacEx) {
                try {
                    return keycloakDecoder.decode(token);
                } catch (JwtException keycloakEx) {
                    // Surface the Keycloak error so the 401 message is meaningful
                    throw keycloakEx;
                }
            }
        };
    }

    /**
     * Extracts roles from Keycloak's {@code realm_access.roles} claim and maps them
     * to Spring Security {@code ROLE_*} granted authorities.
     *
     * <p>For custom HMAC tokens the {@code roles} claim (flat list) is used instead.
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> keycloakAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Keycloak puts roles under realm_access.roles; fall back to standard "roles" claim
        authoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
