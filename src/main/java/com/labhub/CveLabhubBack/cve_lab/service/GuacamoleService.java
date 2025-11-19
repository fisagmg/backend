package com.labhub.CveLabhubBack.cve_lab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labhub.CveLabhubBack.cve_lab.client.GuacamoleClient;
import com.labhub.CveLabhubBack.cve_lab.config.GuacamoleConfig;
import com.labhub.CveLabhubBack.cve_lab.dto.LabCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Guacamole 서비스
 * - Connection 생성 및 삭제
 * - 서비스 계정(guacadmin)만 사용하여 모든 connection 관리
 * 
 * 구조:
 * - Guacamole에는 서비스 계정(guacadmin) 1개만 존재
 * - 사용자별 Guacamole 계정을 생성하지 않음
 * - 모든 connection은 서비스 계정으로 생성하고 관리
 * - 모든 접근은 guacadmin 토큰으로 이루어짐
 * - 사용자 구분, 권한, 만료 시간은 백엔드 DB에서 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuacamoleService {

    private final GuacamoleAuthTokenService authTokenService;
    private final GuacamoleClient guacamoleClient;
    private final GuacamoleConfig guacamoleConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // /**
    //  * 서비스 계정으로 Connection을 생성하고 connectionId를 반환합니다.
    //  * 사용자별 권한 부여는 하지 않습니다 (서비스 계정이 모든 connection에 접근 가능).
    //  * 
    //  * @param username Guacamole 사용자명 (Keycloak의 preferred_username 또는 sub)
    //  * @param response Lab 생성 응답 (호스트 정보 포함)
    //  * @return Guacamole connection ID (identifier, 예: "62")
    //  */
    // public String createConnection(LabCreateResponse response) {
    //     log.info("Creating Guacamole connection using service account");
        
    //     // 서비스 계정 토큰 획득
    //     String adminToken = authTokenService.getAdminToken();
        
    //     // Connection 생성 (서비스 계정으로 생성, connectionId 반환)
    //     String connectionId = guacamoleClient.createConnection(adminToken, response);
        
    //     log.info("Guacamole connection created: connectionId={}", connectionId);
    //     return connectionId;
    // }

    // /**
    //  * Guacamole Connection을 삭제합니다.
    //  * 
    //  * @param connectionIdOrName Connection ID (identifier) 또는 name
    //  */
    // public void deleteGuacSession(String connectionIdOrName) {
    //     if (connectionIdOrName == null || connectionIdOrName.isBlank()) {
    //         log.warn("Connection ID or name is empty, skipping deletion");
    //         return;
    //     }

    //     try {
    //         String adminToken = authTokenService.getAdminToken();
    //         // connectionId를 직접 사용 (name이어도 identifier로 사용 가능)
    //         guacamoleClient.deleteConnection(adminToken, connectionIdOrName);
    //         log.info("Guacamole session deleted: connectionId={}", connectionIdOrName);
    //     } catch (RuntimeException ex) {
    //         log.warn("Failed to delete Guacamole connection {}: {}", connectionIdOrName, ex.getMessage());
    //         throw ex;
    //     }

    
//     }

//     /**
//      * Connection ID와 authToken을 사용하여 iframe URL을 생성합니다.
//      * 
//      * @param connectionId Connection ID (identifier, 예: "62")
//      * @return iframe URL with token (예: "http://172.16.4.10:8080/guacamole/#/client/62?token=...")
//      */
//     public String buildIframeUrl(String connectionId) {
//         // 서비스 계정 토큰 획득
//         String authToken = authTokenService.getAdminToken();
        
//         String baseUrl = guacamoleConfig.getIframeBaseUrl();
//         // baseUrl이 슬래시로 끝나지 않으면 추가
//         if (!baseUrl.endsWith("/")) {
//             baseUrl = baseUrl + "/";
//         }
        
//         // ConnectionId와 token을 사용하여 URL 생성
//         // baseUrl이 이미 "/"로 끝나므로 "#/client/"를 바로 붙임
//         String url = baseUrl + "#/client/" + connectionId + "?token=" + authToken;
//         log.info("✅ Iframe URL created with token: connectionId={}, url={}", 
//                 connectionId, url);
//         return url;
// }

/**
 * 서비스 계정(guacadmin)으로 Connection을 생성하고 connectionId를 반환합니다.
 * 
 * 사용자별 Guacamole 계정 생성이나 권한 부여는 하지 않습니다.
 * 모든 접근은 guacadmin 토큰으로 이루어집니다.
 * 
 * @param response Lab 생성 응답 (호스트 정보 포함)
 * @param username Guacamole 사용자명 (로깅용, 실제로는 사용하지 않음)
 * @return Guacamole connection ID (identifier, 예: "82")
 */
public String createConnection(LabCreateResponse response, String username) {
    log.info("Creating Guacamole connection using service account (guacadmin)");
    
    // 서비스 계정 토큰 획득
    String adminToken = authTokenService.getAdminToken();
    
    // Connection 생성만 수행 (사용자 생성/권한 부여 제거)
    String connectionId = guacamoleClient.createConnection(adminToken, response);
    
    // connectionId 검증
    if (connectionId == null || connectionId.isBlank()) {
        log.error("❌ Connection ID is null or empty after creation!");
        throw new IllegalStateException("Guacamole connection creation returned null or empty connectionId");
    }
    
    log.info("✅ Guacamole connection created successfully: connectionId={}", connectionId);
    return connectionId;
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
        String adminToken = authTokenService.getAdminToken();
        // connectionId를 직접 사용 (name이어도 identifier로 사용 가능)
        guacamoleClient.deleteConnection(adminToken, connectionIdOrName);
        log.info("Guacamole session deleted: connectionId={}", connectionIdOrName);
    } catch (RuntimeException ex) {
        log.warn("Failed to delete Guacamole connection {}: {}", connectionIdOrName, ex.getMessage());
        throw ex;
    }
}

/**
 * Connection ID와 서비스 계정 토큰을 사용하여 iframe URL을 생성합니다.
 * 
 * 서비스 계정 토큰을 URL에 포함시켜 자동 로그인되도록 합니다.
 * 
 * 중요: Guacamole는 /api/tunnels 엔드포인트가 없습니다. 대신 Base64로 인코딩된 clientId를 사용합니다.
 * 형식: "<connectionId>\0c\0<datasource>"를 Base64 인코딩하여 사용합니다.
 * 
 * @param connectionId Connection ID (identifier, 예: "101")
 * @param username Guacamole 사용자명 (Keycloak의 preferred_username) - 로깅용
 * @return iframe URL with token (예: "http://172.16.4.10:8080/guacamole/#/client/MTAxAGMA...?token=...")
 *         clientId는 Base64 인코딩된 "<connectionId>\0c\0<datasource>" 형식
 */
public String buildIframeUrl(String connectionId, String username) {
    log.info("Building iframe URL for connectionId={}, username={}", connectionId, username);
    
    // connectionId 검증
    if (connectionId == null || connectionId.isBlank()) {
        log.error("❌ Connection ID is null or empty! Cannot build iframe URL.");
        throw new IllegalArgumentException("Connection ID is required to build iframe URL");
    }
    
    // 서비스 계정 토큰 획득
    String authToken = authTokenService.getAdminToken();
    
    // 토큰 정보 로깅
    if (authToken == null || authToken.isBlank()) {
        log.error("❌ Auth token is null or empty!");
        throw new IllegalStateException("Guacamole auth token is null or empty");
    }
    log.info("   Token length: {}", authToken.length());
    log.info("   Token (first 20 chars): {}", authToken.length() > 20 ? authToken.substring(0, 20) + "..." : authToken);
    
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
    
    // 2단계: iframe URL 생성
    String baseUrl = guacamoleConfig.getIframeBaseUrl();
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

/**
 * Connection 정보를 조회하여 반환합니다.
 * 
 * @param token Guacamole 인증 토큰
 * @param connectionId Connection ID
 * @return Connection 정보 Map (identifier 필드 포함)
 */
private Map<String, Object> getConnectionInfo(String token, String connectionId) {
    // connectionId 검증
    if (connectionId == null || connectionId.isBlank()) {
        log.error("❌ Connection ID is null or empty! Cannot get connection info.");
        throw new IllegalArgumentException("Connection ID is required");
    }
    
    String dataSource = guacamoleConfig.getDataSource(); // "mysql"
    String url = guacamoleConfig.getBaseUrl() + "/api/session/data/" + dataSource + "/connections/" 
            + connectionId + "?token=" + token;
    
    log.info("Getting connection info: connectionId={}, dataSource={}", connectionId, dataSource);
    log.info("   URL: {}", url);
    
    try {
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("❌ Failed to get connection info: status={}", response.getStatusCode());
            throw new IllegalStateException("Failed to get connection info: " + response.getStatusCode());
        }
        
        Map<String, Object> connectionInfo = response.getBody();
        if (connectionInfo == null) {
            log.error("❌ Connection info response body is null");
            throw new IllegalStateException("Connection info response body is null");
        }
        
        log.info("✅ Connection info retrieved successfully");
        log.info("   Connection details keys: {}", connectionInfo.keySet());
        
        // 응답 본문 로그 출력 (디버깅용)
        try {
            String responseJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(connectionInfo);
            log.info("=== Guacamole Connection Info Response ===");
            log.info("Response Body:\n{}", responseJson);
        } catch (Exception e) {
            log.warn("Failed to serialize connection info for logging: {}", e.getMessage());
        }
        
        return connectionInfo;
    } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
        log.error("❌ Connection not found or token cannot access: connectionId={}", connectionId);
        log.error("   Error: {}", ex.getMessage());
        log.error("   Response Body: {}", ex.getResponseBodyAsString());
        throw new IllegalStateException("Connection " + connectionId + " not found or not accessible", ex);
    } catch (Exception ex) {
        log.error("❌ Failed to get connection info: {}", ex.getMessage());
        log.error("   This might indicate the token is invalid or connection doesn't exist");
        throw new IllegalStateException("Failed to get connection info: " + connectionId, ex);
    }
}



}
