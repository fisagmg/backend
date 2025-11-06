package com.labhub.CveLabhubBack.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API 서버면 보통 CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // CORS (프론트 도메인 허용)
                .cors(cors -> cors.configurationSource(req -> {
                    var c = new CorsConfiguration();
                    c.setAllowedOrigins(List.of("http://localhost:3000"));
                    c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
                    c.setAllowedHeaders(List.of("Authorization","Content-Type"));
                    c.setAllowCredentials(true);
                    return c;
                }))

                // 인가 규칙
                .authorizeHttpRequests(auth -> auth
                        // ✅ 사전 허용(인증 불필요) 엔드포인트
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health", "/public/**").permitAll()
                        .requestMatchers("/api/v1/auth/otp/**").permitAll()     // OTP 전송/검증
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/signup").permitAll()

                        // 그 외는 인증 필요
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
}
