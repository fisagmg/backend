package com.labhub.CveLabhubBack.config;

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

//    // (옵션) Keycloak role → Spring 권한 매핑
//    private JwtAuthenticationConverter keycloakRoleConverter() {
//        var converter = new JwtAuthenticationConverter();
//        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
//        return converter;
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API이므로 CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // CORS (프론트 도메인 넣어두면 편함)
                .cors(cors -> cors.configurationSource(req -> {
                    var c = new CorsConfiguration();
                    c.setAllowedOrigins(List.of("http://localhost:3000")); // 필요에 맞게
                    c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
                    c.setAllowedHeaders(List.of("Authorization","Content-Type"));
                    c.setAllowCredentials(true);
                    return c;
                }))

                // 요청 권한 매칭
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/signup").permitAll()
                        // ✅ 토큰 발급 및 헬스체크는 열어두기
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/health", "/public/**").permitAll()

                        // (원하면 Swagger도 허용)
                        //.requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()

                        // 그 외는 인증 필수
                        .anyRequest().authenticated()
                )

//                // ✅ Bearer 토큰(JWT) 검증 사용
//                .oauth2ResourceServer(oauth -> oauth
//                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakRoleConverter()))
//                )
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))

                // 세션을 굳이 쓰지 않음 (stateless)
                .sessionManagement(sm -> sm.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS
                ));

        return http.build();
    }
}
