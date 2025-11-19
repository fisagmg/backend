package com.labhub.CveLabhubBack.cve_lab.service;

import com.labhub.CveLabhubBack.cve_lab.config.GuacamoleConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuacamoleAuthTokenService {

    private final GuacamoleConfig guacamoleConfig;
    private final RestTemplate restTemplate;

    // 테스트용: 매번 새 토큰 생성 (캐시 사용 안 함)
    // private volatile String cachedToken;
    // private volatile long tokenIssuedAt;
    // private static final long TOKEN_TTL_MILLIS = 10 * 60 * 1000L; // 10분

    public synchronized String getAdminToken() {
        // 테스트용: 매번 새 토큰 생성 (캐시 사용 안 함)
        // if (cachedToken != null) {
        //     return cachedToken;
        // }

        String url = guacamoleConfig.getBaseUrl() + "/api/tokens";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", guacamoleConfig.getAdminUsername());
        form.add("password", guacamoleConfig.getAdminPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, new HttpEntity<>(form, headers), Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.error("❌ Failed to obtain Guacamole admin token: status={}, body={}", 
                    response.getStatusCode(), response.getBody());
            throw new IllegalStateException("Failed to obtain Guacamole admin token");
        }

        Map<String, Object> responseBody = response.getBody();
        String authToken = (String) responseBody.get("authToken");
        
        // 토큰 발급 로그 상세 출력
        log.info("✅ Guacamole admin token obtained successfully (NEW TOKEN GENERATED)");
        log.info("   Token length: {}", authToken != null ? authToken.length() : 0);
        log.info("   Token (first 20 chars): {}", authToken != null && authToken.length() > 20 
                ? authToken.substring(0, 20) + "..." : authToken);
        log.info("   Full response keys: {}", responseBody.keySet());
        
        // 토큰 유효성 검증
        if (authToken == null || authToken.isBlank()) {
            log.error("❌ Token is null or empty! Response body: {}", responseBody);
            throw new IllegalStateException("Guacamole admin token is null or empty");
        }
        
        // 토큰으로 간단한 API 호출하여 유효성 검증
        validateToken(authToken);
        
        // 테스트용: 캐시 사용 안 함, 매번 새 토큰 반환
        // cachedToken = authToken;
        // tokenIssuedAt = System.currentTimeMillis();
        return authToken;
    }

    // 테스트용: 만료 체크 비활성화
    // private boolean isExpired() {
    //     return System.currentTimeMillis() - tokenIssuedAt > TOKEN_TTL_MILLIS;
    // }
    
    /**
     * 토큰 유효성 검증
     * 토큰으로 간단한 API 호출하여 유효한지 확인
     */
    private void validateToken(String token) {
        try {
            // 토큰으로 사용자 정보 조회 API 호출하여 검증
            String validateUrl = guacamoleConfig.getBaseUrl() + "/api/session/data/mysql/users/" 
                    + guacamoleConfig.getAdminUsername() + "?token=" + token;
            
            log.info("Validating token by calling: {}", validateUrl);
            
            ResponseEntity<Map> validateResponse = restTemplate.getForEntity(validateUrl, Map.class);
            
            if (validateResponse.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Token validation successful - token is valid");
            } else {
                log.warn("⚠️ Token validation returned status: {}", validateResponse.getStatusCode());
            }
        } catch (Exception ex) {
            log.error("❌ Token validation failed: {}", ex.getMessage());
            log.error("   This might indicate the token is invalid or expired");
            // 검증 실패해도 토큰은 반환 (일부 경우 정상 작동할 수 있음)
        }
    }
}