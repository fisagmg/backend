package com.labhub.CveLabhubBack.cve_lab.service;

import com.labhub.CveLabhubBack.cve_lab.client.GuacamoleClient;
import com.labhub.CveLabhubBack.cve_lab.config.GuacamoleConfig;
import com.labhub.CveLabhubBack.cve_lab.dto.LabCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Guacamole 서비스
 * - Connection 생성 및 삭제
 * - 서비스 계정(guac-service)만 사용하여 모든 connection 관리
 * 
 * 구조:
 * - Guacamole에는 서비스 계정 1개만 존재
 * - 수강생별로 Guacamole user를 만들지 않음
 * - 모든 connection은 서비스 계정으로 생성하고 관리
 * - 수강생 구분, 권한, 만료 시간은 백엔드 DB에서 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuacamoleService {

    private final GuacamoleAuthTokenService authTokenService;
    private final GuacamoleClient guacamoleClient;
    private final GuacamoleConfig guacamoleConfig;
    private final RestTemplate restTemplate;

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
 * 서비스 계정으로 Connection을 생성하고, 사용자에게 권한을 부여한 후 connectionId를 반환합니다.
 * 
 * @param response Lab 생성 응답 (호스트 정보 포함)
 * @param username Guacamole 사용자명 (Keycloak의 preferred_username)
 * @return Guacamole connection ID (identifier, 예: "62")
 */
public String createConnection(LabCreateResponse response, String username) {
    log.info("Creating Guacamole connection using service account for user: {}", username);
    
    // 서비스 계정 토큰 획득
    String adminToken = authTokenService.getAdminToken();
    
    // 1. Connection 생성 (서비스 계정으로 생성, connectionId 반환)
    String connectionId = guacamoleClient.createConnection(adminToken, response);
    
    // 2. 사용자 존재 확인 후 생성
    guacamoleClient.ensureUserExists(adminToken, username);
    
    // 3. 사용자에게 connection 권한 부여
    guacamoleClient.grantConnectionPermission(adminToken, username, connectionId);
    
    log.info("Guacamole connection created: connectionId={}, username={}", connectionId, username);
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
 * @param connectionId Connection ID (identifier, 예: "74")
 * @param username Guacamole 사용자명 (Keycloak의 preferred_username) - 로깅용
 * @return iframe URL with token (예: "http://172.16.4.10:8080/guacamole/#/client/74?token=...")
 */
public String buildIframeUrl(String connectionId, String username) {
    log.info("Building iframe URL for connectionId={}, username={}", connectionId, username);
    
    // 서비스 계정 토큰 획득
    String authToken = authTokenService.getAdminToken();
    
    // 토큰 정보 로깅
    if (authToken == null || authToken.isBlank()) {
        log.error("❌ Auth token is null or empty!");
        throw new IllegalStateException("Guacamole auth token is null or empty");
    }
    log.info("   Token length: {}", authToken.length());
    log.info("   Token (first 20 chars): {}", authToken.length() > 20 ? authToken.substring(0, 20) + "..." : authToken);
    
    // 토큰으로 connection 접근 권한 확인
    validateConnectionAccess(authToken, connectionId);
    
    String baseUrl = guacamoleConfig.getIframeBaseUrl();
    if (!baseUrl.endsWith("/")) {
        baseUrl = baseUrl + "/";
    }
    
    // connectionId와 서비스 계정 토큰을 사용하여 URL 생성
    String url = baseUrl + "#/client/" + connectionId + "?token=" + authToken;
    log.info("✅ Iframe URL created with token");
    log.info("   ConnectionId: {}", connectionId);
    log.info("   Full URL: {}", url);
    log.info("   URL length: {}", url.length());
    return url;
}

/**
 * 토큰으로 connection에 접근할 수 있는지 확인
 */
private void validateConnectionAccess(String token, String connectionId) {
    try {
        String validateUrl = guacamoleConfig.getBaseUrl() + "/api/session/data/mysql/connections/" 
                + connectionId + "?token=" + token;
        
        log.info("Validating connection access: connectionId={}", connectionId);
        log.info("   Validation URL: {}", validateUrl);
        
        ResponseEntity<Map> response = restTemplate.getForEntity(validateUrl, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("✅ Connection access validation successful - token can access connection {}", connectionId);
            if (response.getBody() != null) {
                log.info("   Connection details: {}", response.getBody().keySet());
            }
        } else {
            log.warn("⚠️ Connection access validation returned status: {}", response.getStatusCode());
            log.warn("   This might indicate the token cannot access this connection");
        }
    } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
        log.error("❌ Connection not found or token cannot access: connectionId={}", connectionId);
        log.error("   Error: {}", ex.getMessage());
    } catch (Exception ex) {
        log.error("❌ Connection access validation failed: {}", ex.getMessage());
        log.error("   This might indicate the token is invalid or connection doesn't exist");
    }
}



}
