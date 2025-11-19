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

import java.util.HashMap;
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
        
        // 실제 전송될 JSON 본문 생성 및 로그 출력
        try {
            // 실제 전송될 JSON 생성 (attributes는 빈 Map으로 포함)
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("name", request.name());
            requestMap.put("parentIdentifier", request.parentIdentifier());
            requestMap.put("protocol", request.protocol());
            requestMap.put("parameters", request.parameters());
            
            // attributes는 빈 Map으로 설정 (Guacamole이 NPE를 방지하기 위해 필요)
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("max-connections", "");
            attributes.put("max-connections-per-user", "");
            attributes.put("weight", "");
            attributes.put("failover-only", "");
            attributes.put("guacd-encryption", "");
            attributes.put("guacd-hostname", "");
            attributes.put("guacd-port", "");
            requestMap.put("attributes", attributes);
            
            String actualRequestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestMap);
            // SSH 키 부분 마스킹 (보안)
            String maskedJson = actualRequestJson.replaceAll("\"private-key\"\\s*:\\s*\"[^\"]+\"", "\"private-key\": \"[MASKED]\"");
            
            log.info("=== Guacamole Connection Create Request ===");
            log.info("URL: {}", url);
            log.info("Request Body (masked):\n{}", maskedJson);
            
            // 실제 전송될 JSON 확인 (attributes 포함 여부)
            if (actualRequestJson.contains("\"attributes\"")) {
                log.info("✅ 'attributes' field is correctly included in request JSON (empty Map)");
            } else {
                log.warn("⚠️ WARNING: 'attributes' field is missing from request JSON!");
            }
            
            // SSH 키 검증 로그
            if (response.privateKey() != null && !response.privateKey().isBlank()) {
                String privateKey = response.privateKey();
                boolean hasBegin = privateKey.contains("BEGIN");
                boolean hasEnd = privateKey.contains("END");
                log.info("SSH Key validation: hasBegin={}, hasEnd={}, length={}", hasBegin, hasEnd, privateKey.length());
                if (!hasBegin || !hasEnd) {
                    log.warn("⚠️ SSH Key format may be invalid! Missing BEGIN or END markers.");
                }
            } else {
                log.warn("⚠️ WARNING: privateKey is null or blank!");
            }
            
            // 실제 전송할 HttpEntity 생성 (Map 사용)
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestMap, headers);
            
            // 먼저 Map으로 응답을 받아서 상태 코드와 본문을 확인
            ResponseEntity<Map> resp;
            try {
                resp = restTemplate.postForEntity(
                        url,
                        httpEntity,
                        Map.class
                );
            } catch (org.springframework.web.client.HttpServerErrorException ex) {
                // 500 오류 응답 본문 로그 출력
                log.error("❌ Guacamole Connection Creation Failed (500)");
                log.error("   Status: {}", ex.getStatusCode());
                log.error("   Response Body: {}", ex.getResponseBodyAsString());
                log.error("   Status Text: {}", ex.getStatusText());
                throw new GuacamoleClientException(
                        "Failed to create Guacamole connection: " + ex.getStatusCode() + 
                        " - " + ex.getResponseBodyAsString(), ex);
            } catch (org.springframework.web.client.HttpClientErrorException ex) {
                // 4xx 오류 응답 본문 로그 출력
                log.error("❌ Guacamole Connection Creation Failed (4xx)");
                log.error("   Status: {}", ex.getStatusCode());
                log.error("   Response Body: {}", ex.getResponseBodyAsString());
                log.error("   Status Text: {}", ex.getStatusText());
                throw new GuacamoleClientException(
                        "Failed to create Guacamole connection: " + ex.getStatusCode() + 
                        " - " + ex.getResponseBodyAsString(), ex);
            }

            // 응답 상태 코드 확인
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.error("❌ Unexpected response status: {}", resp.getStatusCode());
                log.error("   Response Body: {}", resp.getBody());
                throw new GuacamoleClientException("Failed to create Guacamole connection: " + resp.getStatusCode());
            }

            // 응답 본문 확인
            Map<String, Object> body = resp.getBody();
            if (body == null) {
                log.error("❌ Response body is null");
                throw new GuacamoleClientException("Guacamole connection creation returned null response body");
            }

            // 응답 본문 전체 로그 출력 (디버깅용)
            try {
                String responseJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
                log.info("=== Guacamole Connection Create Response ===");
                log.info("Response Body:\n{}", responseJson);
            } catch (Exception e) {
                log.warn("Failed to serialize response for logging: {}", e.getMessage());
                log.info("Response Body (raw): {}", body);
            }

            // identifier 추출
            Object identifierObj = body.get("identifier");
            if (identifierObj == null) {
                log.error("❌ Response body does not contain 'identifier' field");
                log.error("   Available keys: {}", body.keySet());
                throw new GuacamoleClientException("Missing 'identifier' field in Guacamole connection response");
            }

            String identifier = identifierObj.toString();
            if (identifier.isBlank()) {
                log.error("❌ Identifier is blank");
                throw new GuacamoleClientException("Guacamole connection identifier is blank");
            }
            
            log.info("✅ Guacamole connection created: connectionId={}, name={}", identifier, request.name());
            return identifier;
            
        } catch (GuacamoleClientException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Failed to create Guacamole connection: {}", e.getMessage(), e);
            throw new GuacamoleClientException("Failed to create Guacamole connection: " + e.getMessage(), e);
        }
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
     * POST /api/tunnels?token={adminToken}
     * Content-Type: application/x-www-form-urlencoded
     * Body: GUAC_ID={connectionId}
     * 
     * 응답 예시:
     * {
     *   "identifier": "c/3c1f0a6b-4d21-4d11-9a6f-3d8b0c9df123",
     *   "active": true,
     *   "readOnly": false
     * }
     * 
     * @param adminToken Guacamole admin 토큰
     * @param connectionId Connection ID (identifier, 예: "96")
     * @return tunnel identifier (예: "c/3c1f0a6b-4d21-4d11-9a6f-3d8b0c9df123")
     */
    public String connectToConnection(String adminToken, String connectionId) {
        if (connectionId == null || connectionId.isBlank()) {
            throw new IllegalArgumentException("Connection ID is required");
        }

        String url = UriComponentsBuilder.fromHttpUrl(guacamoleConfig.getBaseUrl())
                .path("/api/tunnels")
                .queryParam("token", adminToken)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Form data: GUAC_ID=connectionId, GUAC_DATA_SOURCE=dataSource
        String dataSource = guacamoleConfig.getDataSource();
        org.springframework.util.LinkedMultiValueMap<String, String> form = new org.springframework.util.LinkedMultiValueMap<>();
        form.add("GUAC_ID", connectionId);
        form.add("GUAC_DATA_SOURCE", dataSource);

        HttpEntity<org.springframework.util.MultiValueMap<String, String>> entity = 
                new HttpEntity<>(form, headers);

        log.info("Calling /api/tunnels API: connectionId={}, dataSource={}, url={}", connectionId, dataSource, url);
        log.info("   Request body: GUAC_ID={}, GUAC_DATA_SOURCE={}", connectionId, dataSource);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("❌ Failed to open tunnel: status={}, body={}", 
                        response.getStatusCode(), response.getBody());
                throw new GuacamoleClientException(
                        "Failed to open tunnel: " + response.getStatusCode());
            }

            Map<String, Object> body = response.getBody();
            
            // 응답 본문 로그 출력
            try {
                String responseJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
                log.info("=== Guacamole Tunnel Open Response ===");
                log.info("Response Body:\n{}", responseJson);
            } catch (Exception e) {
                log.warn("Failed to serialize tunnel response for logging: {}", e.getMessage());
                log.info("Response Body (raw): {}", body);
            }

            String tunnelIdentifier = (String) body.get("identifier");
            
            if (tunnelIdentifier == null || tunnelIdentifier.isBlank()) {
                log.error("❌ Missing 'identifier' field in tunnel response");
                log.error("   Available keys: {}", body.keySet());
                throw new GuacamoleClientException("Missing tunnel identifier in tunnel response");
            }

            log.info("✅ Tunnel identifier obtained: connectionId={}, tunnelIdentifier={}", 
                    connectionId, tunnelIdentifier);
            return tunnelIdentifier;
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            log.error("❌ Failed to open tunnel: status={}", ex.getStatusCode());
            log.error("   Response Body: {}", ex.getResponseBodyAsString());
            log.error("   URL: {}", url);
            throw new GuacamoleClientException(
                    "Failed to open tunnel: " + ex.getStatusCode() + 
                    " - " + ex.getResponseBodyAsString(), ex);
        } catch (org.springframework.web.client.RestClientException ex) {
            log.error("Failed to open tunnel for connection {}: {}", connectionId, ex.getMessage(), ex);
            throw new GuacamoleClientException(
                    "Failed to open tunnel for connection: " + connectionId, ex);
        }
    }
}