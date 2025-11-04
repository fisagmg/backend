package com.labhub.CveLabhubBack.controller;

import com.labhub.CveLabhubBack.dto.LoginRequestDto;
import com.labhub.CveLabhubBack.dto.RegisterRequestDto;
import com.labhub.CveLabhubBack.service.AuthFlowService;
import com.labhub.CveLabhubBack.service.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

/**
 * OIDC 인증 플로우 구성
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AuthFlowController {

    private final AuthFlowService authFlowService;
    private final KeycloakAdminService keycloakAdminService;

    // 실제로는 application.yml 등 설정 파일에서 불러오는 게 맞음 (테스트용 하드코딩)
    private static final String GRANT_TYPE = "password";
    private static final String CLIENT_ID = "labhub-admin";
    private static final String CLIENT_SECRET = "fTjPQl0mkwkUi3qehOdprRiSjZlRP53Y";

    /**
     * Direct Access Grants Flow : 토큰을 즉시 요청하는 방법
     * @return Keycloak에서 발급한 토큰 값 반환
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
        try {
            log.info("[LOGIN] going to Keycloak with username={}, password={}",
                    request.getUsername(), request.getPassword());

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", GRANT_TYPE);
            form.add("client_id", CLIENT_ID);
            form.add("client_secret", CLIENT_SECRET);
            form.add("username", request.getUsername());
            form.add("password", request.getPassword());

            log.debug("[LOGIN-DEBUG] Sending to Keycloak: "
                            + "grant_type={}, client_id={}, client_secret=****, username={}, password=****",
                    GRANT_TYPE, CLIENT_ID, request.getUsername());

            Object tokenResponse = authFlowService.getAccessToken(
                    GRANT_TYPE,
                    CLIENT_ID,
                    CLIENT_SECRET,
                    request.getUsername(),
                    request.getPassword()
            );

            log.info("[LOGIN] Keycloak response = {}", tokenResponse);
            return ResponseEntity.ok(tokenResponse);

        } catch (Exception e) {
            log.error("[LOGIN] Keycloak rejected login", e);
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    /** 회원가입 → Keycloak에 사용자 생성 */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody RegisterRequestDto req) {
        if (req.getEmail() == null || req.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "email과 password, firstName, lastName는 필수입니다."
            ));
        }

        try {
            String userId = keycloakAdminService.createUser(
                    req.getEmail(),
                    req.getPassword(),
                    req.getFirstName(),
                    req.getLastName(),
                    req.getPhone()
            );

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "userId", userId,
                    "email", req.getEmail()
            ));

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 409) {
                return ResponseEntity.status(409).body(Map.of(
                        "status", "ERROR",
                        "message", "이미 존재하는 사용자입니다."
                ));
            }
            return ResponseEntity.status(502).body(Map.of(
                    "status", "ERROR",
                    "message", "인증 서버 오류: " + e.getStatusCode()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "회원가입 처리 중 오류가 발생했습니다."
            ));
        }
    }
}
