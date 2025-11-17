package com.labhub.CveLabhubBack.cve_lab.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labhub.CveLabhubBack.cve_lab.config.GuacamoleConfig;
import com.labhub.CveLabhubBack.cve_lab.dto.GuacamoleConnectionRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.GuacamoleConnectionResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.GuacamoleUserRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.LabCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuacamoleClient {

    private final GuacamoleConfig guacamoleConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

     /**
     * Connection을 생성하고 connectionId를 반환합니다.
     * @param adminToken Guacamole admin 토큰
     * @param response Lab 생성 응답 (호스트 정보 포함)
     * @return connectionId (identifier)
     */
    public String createConnection(String adminToken, LabCreateResponse response) {
        String url = UriComponentsBuilder.fromHttpUrl(guacamoleConfig.getBaseUrl())
                .path("/api/session/data/mysql/connections")
                .queryParam("token", adminToken)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        GuacamoleConnectionRequest request = GuacamoleConnectionRequest.from(response);
        
        // 요청 로그 출력
        try {
            String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            log.info("=== Guacamole Connection Create Request ===");
            log.info("URL: {}", url);
            log.info("Request Body:\n{}", requestJson);
        } catch (Exception e) {
            log.warn("Failed to serialize request for logging: {}", e.getMessage());
        }

        ResponseEntity<GuacamoleConnectionResponse> resp =
                restTemplate.postForEntity(
                        url,
                        new HttpEntity<>(request, headers),
                        GuacamoleConnectionResponse.class
                );

        GuacamoleConnectionResponse body = resp.getBody();
        if (!resp.getStatusCode().is2xxSuccessful() || body == null) {
            throw new GuacamoleClientException("Failed to create Guacamole connection: " + resp.getStatusCode());
        }
        
        // connectionId (identifier) 반환
        if (body.identifier() == null || body.identifier().isBlank()) {
            throw new GuacamoleClientException("Missing Guacamole connection identifier");
        }
        
        log.info("Guacamole connection created: connectionId={}", body.identifier());
        return body.identifier();
    }

    /**
     * Guacamole 사용자 존재 여부 확인 후, 없으면 생성
     */
    public void ensureUserExists(String adminToken, String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Guacamole username is required.");
        }

        String dataSource = guacamoleConfig.getDataSource();
        String baseUrl = guacamoleConfig.getBaseUrl();

        String userUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/session/data/")
                .path(dataSource)
                .path("/users/")
                .path(username)
                .queryParam("token", adminToken)
                .toUriString();

        try {
            restTemplate.getForEntity(userUrl, Void.class);
            log.debug("Guacamole user already exists: {}", username);
            return;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
            log.info("Guacamole user not found. Creating: {}", username);
        }

        String createUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/session/data/")
                .path(dataSource)
                .path("/users")
                .queryParam("token", adminToken)
                .toUriString();

        GuacamoleUserRequest request = GuacamoleUserRequest.from(username);
        restTemplate.postForEntity(createUrl, request, Void.class);
        log.info("Guacamole user created via REST: {}", username);
    }

    /**
     * 특정 사용자에게 Connection 접근 권한을 부여합니다.
     * PATCH /api/session/data/mysql/users/{username}/permissions?token={adminToken}
     * Body: [{"op": "add", "path": "/connectionPermissions/{connectionId}", "value": "READ"}]
     * 
     * @param adminToken Guacamole admin 토큰
     * @param username Guacamole 사용자명 (Keycloak의 preferred_username 또는 sub)
     * @param connectionId Connection ID (identifier)
     */
    public void grantConnectionPermission(String adminToken, String username, String connectionId) {
        String url = UriComponentsBuilder.fromHttpUrl(guacamoleConfig.getBaseUrl())
                .path("/api/session/data/mysql/users/" + username + "/permissions")
                .queryParam("token", adminToken)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // PATCH 요청 Body: [{"op": "add", "path": "/connectionPermissions/<connectionId>", "value": "READ"}]
        List<Map<String, Object>> patchOperations = List.of(
                Map.of(
                        "op", "add",
                        "path", "/connectionPermissions/" + connectionId,
                        "value", "READ"
                )
        );

        // 요청 로그 출력
        try {
            String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(patchOperations);
            log.info("=== Guacamole Permission Grant Request ===");
            log.info("URL: {}", url);
            log.info("Username: {}, ConnectionId: {}", username, connectionId);
            log.info("Request Body:\n{}", requestJson);
        } catch (Exception e) {
            log.warn("Failed to serialize permission request for logging: {}", e.getMessage());
        }

        ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.PATCH,
                new HttpEntity<>(patchOperations, headers),
                Void.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new GuacamoleClientException("Failed to grant connection permission: " + response.getStatusCode());
        }
        
        log.info("Guacamole permission granted: username={}, connectionId={}", username, connectionId);
    }

    /**
     * Connection을 삭제합니다.
     * @param adminToken Guacamole admin 토큰
     * @param connectionId Connection ID (identifier)
     */
    public void deleteConnection(String adminToken, String connectionId) {
        if (connectionId == null || connectionId.isBlank()) return;

        String url = UriComponentsBuilder.fromHttpUrl(guacamoleConfig.getBaseUrl())
                .path("/api/session/data/mysql/connections/")
                .path(connectionId)
                .queryParam("token", adminToken)
                .toUriString();

        log.info("Deleting Guacamole connection: connectionId={}", connectionId);
        restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(new HttpHeaders()), Void.class);
        log.info("Guacamole connection deleted: connectionId={}", connectionId);
    }
}