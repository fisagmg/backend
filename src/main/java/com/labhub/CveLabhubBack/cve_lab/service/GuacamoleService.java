package com.labhub.CveLabhubBack.cve_lab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labhub.CveLabhubBack.cve_lab.config.GuacamoleConfig;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabCreateResponse;
import com.labhub.CveLabhubBack.cve_lab.entity.Lab;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuacamoleService {

    private final GuacamoleConfig guacamoleConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 서비스 계정(guacadmin) 토큰 획득
     */
    private synchronized String getAdminToken() {
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
        
        if (authToken == null || authToken.isBlank()) {
            log.error("❌ Token is null or empty! Response body: {}", responseBody);
            throw new IllegalStateException("Guacamole admin token is null or empty");
        }
        
        log.info("✅ Guacamole admin token obtained successfully");
        return authToken;
    }

    /**
     * 서비스 계정(guacadmin)으로 Connection을 생성하고 connectionId를 반환
     * 사용자별 Guacamole 계정 생성이나 권한 부여는 하지 않음
     */
    public String createConnection(LabCreateResponse response) {
        log.info("Creating Guacamole connection using service account (guacadmin)");

        // 서비스 계정 토큰 획득
        String adminToken = getAdminToken();

        // API URL 생성
        String url = UriComponentsBuilder.fromHttpUrl(guacamoleConfig.getBaseUrl())
                .path("/api/session/data/mysql/connections")
                .queryParam("token", adminToken)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 요청 본문 생성
        Map<String, Object> requestMap = buildConnectionRequestMap(response);

        // 요청 로그 출력 (SSH 키 마스킹)
        try {
            String actualRequestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestMap);
            String maskedJson = actualRequestJson.replaceAll("\"private-key\"\\s*:\\s*\"[^\"]+\"", "\"private-key\": \"[MASKED]\"");
            
            log.info("=== Guacamole Connection Create Request ===");
            log.info("URL: {}", url);
            log.info("Request Body (masked):\n{}", maskedJson);
        } catch (Exception e) {
            log.warn("Failed to serialize request for logging: {}", e.getMessage());
        }

        // HTTP 요청 전송
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestMap, headers);
        
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, httpEntity, Map.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.error("❌ Unexpected response status: {}", resp.getStatusCode());
                throw new IllegalStateException("Failed to create Guacamole connection: " + resp.getStatusCode());
            }

            Map<String, Object> body = resp.getBody();
            if (body == null) {
                log.error("❌ Response body is null");
                throw new IllegalStateException("Guacamole connection creation returned null response body");
            }

            // connectionId 추출
            Object identifierObj = body.get("identifier");
            if (identifierObj == null) {
                log.error("❌ Response body does not contain 'identifier' field. Available keys: {}", body.keySet());
                throw new IllegalStateException("Missing 'identifier' field in Guacamole connection response");
            }

            String connectionId = identifierObj.toString();
            if (connectionId.isBlank()) {
                log.error("❌ Identifier is blank");
                throw new IllegalStateException("Guacamole connection identifier is blank");
            }

            log.info("✅ Guacamole connection created successfully: connectionId={}", connectionId);
            return connectionId;

        } catch (org.springframework.web.client.HttpServerErrorException ex) {
            log.error("❌ Guacamole Connection Creation Failed (500)");
            log.error("   Status: {}", ex.getStatusCode());
            log.error("   Response Body: {}", ex.getResponseBodyAsString());
            throw new IllegalStateException("Failed to create Guacamole connection: " + ex.getStatusCode(), ex);
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            log.error("❌ Guacamole Connection Creation Failed (4xx)");
            log.error("   Status: {}", ex.getStatusCode());
            log.error("   Response Body: {}", ex.getResponseBodyAsString());
            throw new IllegalStateException("Failed to create Guacamole connection: " + ex.getStatusCode(), ex);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to create Guacamole connection: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Failed to create Guacamole connection: " + ex.getMessage(), ex);
        }
    }

    /**
     * LabCreateResponse를 Guacamole Connection 요청 Map으로 변환
     */
    private Map<String, Object> buildConnectionRequestMap(LabCreateResponse response) {
        // SSH 파라미터 구성
        Map<String, Object> params = new HashMap<>();
        params.put("hostname", response.privateIp());
        params.put("port", "22");
        params.put("username", response.sshUsername());

        // SSH 키 설정 및 정규화
        if (response.privateKey() != null && !response.privateKey().isBlank()) {
            String privateKey = response.privateKey();
            privateKey = privateKey.replace("\\n", "\n");
            params.put("private-key", privateKey);
        }

        if (response.sshPassword() != null && !response.sshPassword().isBlank()) {
            params.put("password", response.sshPassword());
        }

        // attributes는 빈 Map으로 설정 (Guacamole이 NPE를 방지하기 위해 필요)
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("max-connections", "");
        attributes.put("max-connections-per-user", "");
        attributes.put("weight", "");
        attributes.put("failover-only", "");
        attributes.put("guacd-encryption", "");
        attributes.put("guacd-hostname", "");
        attributes.put("guacd-port", "");

        // 요청 Map 구성
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("name", response.uuid());
        requestMap.put("parentIdentifier", "ROOT");
        requestMap.put("protocol", "ssh");
        requestMap.put("parameters", params);
        requestMap.put("attributes", attributes);

        return requestMap;
    }

    /**
     * Guacamole Connection을 삭제합니다.
     *
     * @param connectionIdOrName Connection ID (identifier) 또는 name
     */
    public void deleteGuacSession(String connectionIdOrName) {
        if (connectionIdOrName == null || connectionIdOrName.isBlank()) {
            log.warn("Connection ID or name is empty, skipping deletion");
            return;
        }

        try {
            String adminToken = getAdminToken();
            
            String url = UriComponentsBuilder.fromHttpUrl(guacamoleConfig.getBaseUrl())
                    .path("/api/session/data/mysql/connections/")
                    .path(connectionIdOrName)
                    .queryParam("token", adminToken)
                    .toUriString();

            log.info("Deleting Guacamole connection: connectionId={}", connectionIdOrName);
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(new HttpHeaders()), Void.class);
            log.info("Guacamole session deleted: connectionId={}", connectionIdOrName);
        } catch (Exception ex) {
            log.warn("Failed to delete Guacamole connection {}: {}", connectionIdOrName, ex.getMessage());
            throw new IllegalStateException("Failed to delete Guacamole connection: " + connectionIdOrName, ex);
        }
    }

    /**
     * Lab 엔티티의 Guacamole connection을 삭제합니다.
     * connectionId가 없거나 비어있으면 아무 작업도 하지 않습니다.
     * 
     * @param lab Lab 엔티티
     * @return 삭제 성공 여부
     */
    public boolean deleteConnectionForLab(Lab lab) {
        if (lab == null) {
            log.warn("Lab is null, skipping Guacamole connection deletion");
            return false;
        }

        String connectionId = lab.getGuacamoleConnectionId();
        if (connectionId == null || connectionId.isBlank()) {
            log.warn("Guacamole connectionId not found for lab uuid: {}", lab.getUuid());
            return false;
        }

        try {
            deleteGuacSession(connectionId);
            log.info("Guacamole connection deleted for lab: uuid={}, connectionId={}", 
                    lab.getUuid(), connectionId);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to delete Guacamole connection for lab uuid {}: {}", 
                    lab.getUuid(), ex.getMessage());
            return false;
        }
    }

    public String buildIframeUrl(String connectionId, String username) {
        log.info("Building iframe URL for connectionId={}, username={}", connectionId, username);

        // connectionId 검증
        if (connectionId == null || connectionId.isBlank()) {
            log.error("❌ Connection ID is null or empty! Cannot build iframe URL.");
            throw new IllegalArgumentException("Connection ID is required to build iframe URL");
        }

        // 서비스 계정 토큰 획득
        String authToken = getAdminToken();

        // 1단계: Base64 인코딩된 clientId 생성
        // 형식: "<connectionId>\0c\0<datasource>"
        String dataSource = guacamoleConfig.getDataSource(); // "mysql"
        String rawClientId = connectionId + "\0" + "c" + "\0" + dataSource;

        log.info("Creating Base64 clientId: connectionId={}, dataSource={}", connectionId, dataSource);
        log.info("   Raw clientId string: {} (with null bytes)", rawClientId.replace("\0", "\\0"));

        // Base64 인코딩
        String clientId = Base64.getEncoder()
                .encodeToString(rawClientId.getBytes(StandardCharsets.UTF_8));

        log.info("✅ Base64 clientId created: {}", clientId);

        // 2단계: iframe URL 생성 http://172.16.4.10:8080/guacamole
        String baseUrl = guacamoleConfig.getBaseUrl();
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        // baseUrl에서 /guacamole 부분 제거 (이미 포함되어 있을 수 있음)
        String guacBaseUrl = baseUrl.replaceAll("/guacamole/?$", "");
        if (!guacBaseUrl.endsWith("/")) {
            guacBaseUrl = guacBaseUrl + "/";
        }

        // iframe URL 생성: #/client/{base64-clientId}?token={token}
        String fragment = String.format("#/client/%s?token=%s", clientId, authToken);
        String url = guacBaseUrl + "guacamole/" + fragment;

        log.info("✅ Iframe URL created");
        log.info("   ConnectionId (DB): {}", connectionId);
        log.info("   Base64ClientId: {}", clientId);
        log.info("   DataSource: {}", dataSource);
        log.info("   Full URL: {}", url);
        log.info("   URL length: {}", url.length());
        return url;
    }
}

