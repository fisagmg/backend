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
     * Connection을 생성하고 connectionId (identifier)를 반환합니다.
     * @param adminToken Guacamole admin 토큰
     * @param response Lab 생성 응답 (호스트 정보 포함)
     * @return connectionId (identifier, 예: "62")
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
        
        // connectionId (identifier) 반환 - URL 접속에 사용됨
        if (body.identifier() == null || body.identifier().isBlank()) {
            throw new GuacamoleClientException("Missing Guacamole connection identifier");
        }
        
        log.info("Guacamole connection created: connectionId={}, name={}", 
                body.identifier(), request.name());
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
     * 특정 사용자에게 Connection 접근 권한을 모두 부여합니다.
     * ConnectionId (identifier)를 사용합니다.
     * 
     * @param adminToken Guacamole admin 토큰
     * @param username Guacamole 사용자명 (Keycloak의 preferred_username 또는 sub)
     * @param connectionId Connection ID (identifier, 예: "62")
     */
    public void grantConnectionPermission(String adminToken, String username, String connectionId) {
        String url = UriComponentsBuilder.fromHttpUrl(guacamoleConfig.getBaseUrl())
                .path("/api/session/data/mysql/users/" + username + "/permissions")
                .queryParam("token", adminToken)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 모든 권한 부여: READ, UPDATE, DELETE, ADMINISTER
        // connectionId (identifier) 사용
        List<Map<String, Object>> patchOperations = List.of(
                Map.of("op", "add", "path", "/connectionPermissions/" + connectionId, "value", "READ"),
                Map.of("op", "add", "path", "/connectionPermissions/" + connectionId, "value", "UPDATE"),
                Map.of("op", "add", "path", "/connectionPermissions/" + connectionId, "value", "DELETE"),
                Map.of("op", "add", "path", "/connectionPermissions/" + connectionId, "value", "ADMINISTER")
        );

        // 요청 로그 출력
        try {
            String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(patchOperations);
            log.info("=== Guacamole Permission Grant Request (All Permissions) ===");
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
        
        log.info("Guacamole all permissions granted: username={}, connectionId={}", username, connectionId);
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

    /**
     * Connection에 연결하여 tunnel identifier를 반환합니다.
     * 
     * 정식 API 경로:
     * POST /api/session/data/{dataSource}/connections/{connectionId}/connect?token={adminToken}
     * 
     * 응답 예시:
     * {
     *   "identifier": "NTkAYwBteXNxbA"
     * }
     * 
     * @param adminToken Guacamole admin 토큰
     * @param connectionId Connection ID (identifier)
     * @return tunnel identifier (예: "NTkAYwBteXNxbA")
     */
    public String connectToConnection(String adminToken, String connectionId) {
        if (connectionId == null || connectionId.isBlank()) {
            throw new IllegalArgumentException("Connection ID is required");
        }

        String dataSource = guacamoleConfig.getDataSource();
        String url = UriComponentsBuilder.fromHttpUrl(guacamoleConfig.getBaseUrl())
                .path("/api/session/data/")
                .path(dataSource)
                .path("/connections/")
                .path(connectionId)
                .path("/connect")
                .queryParam("token", adminToken)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.info("Calling connect API: connectionId={}, dataSource={}, url={}", 
                connectionId, dataSource, url);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GuacamoleClientException(
                        "Failed to connect to Guacamole connection: " + response.getStatusCode());
            }

            Map<String, Object> body = response.getBody();
            String tunnelIdentifier = (String) body.get("identifier");
            
            if (tunnelIdentifier == null || tunnelIdentifier.isBlank()) {
                throw new GuacamoleClientException("Missing tunnel identifier in connect response");
            }

            log.info("✅ Tunnel identifier obtained: connectionId={}, tunnelIdentifier={}", 
                    connectionId, tunnelIdentifier);
            return tunnelIdentifier;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
            log.error("❌ 404 Not Found - Guacamole connect API endpoint not found: {}", url);
            log.error("Check if dataSource is correct: {}", dataSource);
            log.error("Check if connectionId exists: {}", connectionId);
            log.error("Full error: {}", ex.getResponseBodyAsString());
            throw new GuacamoleClientException(
                    "Guacamole connect API endpoint not found (404). URL: " + url, ex);
        } catch (org.springframework.web.client.RestClientException ex) {
            log.error("Failed to connect to Guacamole connection {}: {}", connectionId, ex.getMessage(), ex);
            throw new GuacamoleClientException(
                    "Failed to connect to Guacamole connection: " + connectionId, ex);
        }
    }
}