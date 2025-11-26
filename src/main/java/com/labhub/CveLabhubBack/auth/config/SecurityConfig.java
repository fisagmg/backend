package com.labhub.CveLabhubBack.auth.config;

import com.labhub.CveLabhubBack.auth.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    // 1. 인증 없이 접근 가능한 경로용 FilterChain (우선순위 높음)
    @Bean
    @Order(1)
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/auth/**", "/actuator/**", "/public/**", "/error", "/api/news/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(req -> {
                    var c = new CorsConfiguration();
                    c.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
                    c.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
                    c.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
                    c.setAllowCredentials(allowCredentials);
                    return c;
                }))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(sm -> sm.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS
                ));

        return http.build();
    }

    // 2. JWT 인증이 필요한 경로용 FilterChain (우선순위 낮음)
    @Bean
    @Order(2)
    public SecurityFilterChain protectedFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(req -> {
                    var c = new CorsConfiguration();
                    c.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
                    c.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
                    c.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
                    c.setAllowCredentials(allowCredentials);
                    return c;
                }))
                .sessionManagement(sm -> sm.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS
                ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/news/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        scopeConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Collection<GrantedAuthority> scopeAuthorities = scopeConverter.convert(jwt);
            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }
            authorities.addAll(extractRealmRoles(jwt));
            authorities.addAll(extractResourceRoles(jwt));
            return authorities;
        });
        return converter;
    }

    private Collection<? extends GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles.stream()
                .map(this::toRoleAuthority)
                .toList();
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null || resourceAccess.isEmpty()) {
            return List.of();
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object client : resourceAccess.values()) {
            if (client instanceof Map<?, ?> clientMap && clientMap.containsKey("roles")) {
                Object rolesObj = clientMap.get("roles");
                if (rolesObj instanceof List<?> roles) {
                    roles.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(this::toRoleAuthority)
                            .forEach(authorities::add);
                }
            }
        }
        return authorities;
    }

    private GrantedAuthority toRoleAuthority(String role) {
        return new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());
    }

    public Long currentUserId(Jwt jwt, UserRepository usersRepo){
        String email = (String) jwt.getClaims().getOrDefault("email",
                jwt.getClaimAsString("preferred_username"));
        return usersRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("등록되지 않은 사용자: " + email))
                .getId();
    }
}