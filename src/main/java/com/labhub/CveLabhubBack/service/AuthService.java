package com.labhub.CveLabhubBack.service;

import com.labhub.CveLabhubBack.client.KeycloakAuthClient;
import com.labhub.CveLabhubBack.dto.AuthSuccessData;
import com.labhub.CveLabhubBack.dto.KeycloakTokenResponse;
import com.labhub.CveLabhubBack.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final KeycloakAuthClient keycloakAuthClient;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret:}")
    private String clientSecret;

    private final String GRANT_TYPE = "password";

    public AuthSuccessData login(LoginRequest request) {

        log.info("[LOGIN TRY] grant_type={}", GRANT_TYPE);
        log.info("[LOGIN TRY] client_id={}", clientId);
        log.info("[LOGIN TRY] client_secret={}", clientSecret);
        log.info("[LOGIN TRY] username={}", request.getEmail());
        log.info("[LOGIN TRY] password={}", request.getPassword());

        KeycloakTokenResponse kcResponse;
        try {
            kcResponse = keycloakAuthClient.getToken(
                    GRANT_TYPE,
                    clientId,
                    clientSecret,
                    request.getEmail(),      // username으로 이메일 사용
                    request.getPassword()
            );
        } catch (Exception e) {
            // Keycloak이 "틀린 비번" 같은 경우 400/401류 에러를 던지면 여기 Exception으로 떨어질 수 있음
            throw new InvalidCredentialException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // Keycloak이 정상 토큰 줬을 때 -> 우리가 약속한 data 형태로 변환
        return AuthSuccessData.builder()
                .accessToken(kcResponse.getAccessToken())
                .refreshToken(kcResponse.getRefreshToken())
                .expiresIn(kcResponse.getExpiresIn())
                .tokenType(kcResponse.getTokenType())
                .build();
    }
}
