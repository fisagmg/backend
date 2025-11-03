package com.labhub.CveLabhubBack.controller;

import com.labhub.CveLabhubBack.dto.LoginRequestDto;
import com.labhub.CveLabhubBack.service.AuthFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

/**
 * OIDC 인증 플로우 구성
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthFlowController {

    private final AuthFlowService authFlowService;

    // 실제로는 application.yml 등 설정 파일에서 불러오는 게 맞음 (테스트용 하드코딩)
    private static final String GRANT_TYPE = "password";
    private static final String CLIENT_ID = "myclient";
    private static final String CLIENT_SECRET = "5494NDr8s6jbvidinFZY0HJagp1MZypZ";

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
}
