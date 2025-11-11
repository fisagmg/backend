package com.labhub.CveLabhubBack.auth.config;

import com.labhub.CveLabhubBack.auth.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;

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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API 서버면 보통 CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // CORS (프론트 도메인 허용)
                // 브라우저가 백엔드(API)를 요청할 때 막히지 않도록, 허용해주는 목록
                .cors(cors -> cors.configurationSource(req -> {
                    var c = new CorsConfiguration();
                    c.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
                    c.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
                    c.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
                    c.setAllowCredentials(allowCredentials);
                    return c;
                }))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health", "/public/**").permitAll()
                        .requestMatchers("/api/v1/auth/otp/**").permitAll()     // OTP 전송/검증
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/signup").permitAll()
                        .requestMatchers("/actuator/prometheus", "/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )

                // 리소스 서버(JWT) 사용 – 위 permitAll 경로는 검증에서 제외됨
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))

                // 세션 비활성(Stateless)
                .sessionManagement(sm -> sm.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS
                ));

        return http.build();
    }

    public Long currentUserId(Jwt jwt, UserRepository usersRepo){
        String email = (String) jwt.getClaims().getOrDefault("email",
                jwt.getClaimAsString("preferred_username")); // fallback
        return usersRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("등록되지 않은 사용자: " + email))
                .getId();
    }
}

