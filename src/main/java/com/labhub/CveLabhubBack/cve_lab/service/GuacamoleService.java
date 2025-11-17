package com.labhub.CveLabhubBack.cve_lab.service;

import com.labhub.CveLabhubBack.cve_lab.client.GuacamoleClient;
import com.labhub.CveLabhubBack.cve_lab.config.GuacamoleConfig;
import com.labhub.CveLabhubBack.cve_lab.dto.LabCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Guacamole 서비스
 * - Connection 생성 및 삭제
 * - 사용자별 권한 부여 (REST API 사용)
 * 
 * 주의: Guacamole 사용자는 Keycloak OIDC를 통해 자동 생성되므로
 * 백엔드에서 사용자를 직접 생성할 필요가 없습니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuacamoleService {

    private final GuacamoleAuthTokenService authTokenService;
    private final GuacamoleClient guacamoleClient;
    private final GuacamoleConfig guacamoleConfig;

    /**
     * 사용자용 Guacamole Connection을 생성하고 권한을 부여합니다.
     * 
     * @param username Guacamole 사용자명 (Keycloak의 preferred_username 또는 sub)
     * @param response Lab 생성 응답 (호스트 정보 포함)
     * @return Guacamole iframe URL
     */
    public String createGuacSession(String username, LabCreateResponse response) {
        log.info("Creating Guacamole session for user: {}", username);
        
        // 1) guacadmin 토큰 획득
        String adminToken = authTokenService.getAdminToken();
        
        // Keycloak 사용자가 Guacamole DB에 존재하는지 확인 (없으면 REST API로 생성)
        guacamoleClient.ensureUserExists(adminToken, username);

        // 2) Connection 생성 (connectionId 반환)
        String connectionId = guacamoleClient.createConnection(adminToken, response);

        // 3) 권한을 해당 유저에게만 부여 (REST API 사용)
        guacamoleClient.grantConnectionPermission(adminToken, username, connectionId);
        
        // 4) iframe URL 생성
        String iframeUrl = buildIframeUrl(connectionId);
        log.info("Guacamole session created: username={}, connectionId={}, iframeUrl={}", 
                username, connectionId, iframeUrl);
        
        return iframeUrl;
    }

    /**
     * Connection ID를 반환하는 메서드 (LabService에서 connectionId 저장용)
     * 
     * @param username Guacamole 사용자명
     * @param response Lab 생성 응답
     * @return Guacamole connection ID (identifier)
     */
    public String createGuacSessionAndGetConnectionId(String username, LabCreateResponse response) {
        log.info("Creating Guacamole session for user: {}", username);
        
        String adminToken = authTokenService.getAdminToken();
        guacamoleClient.ensureUserExists(adminToken, username);
        String connectionId = guacamoleClient.createConnection(adminToken, response);
        guacamoleClient.grantConnectionPermission(adminToken, username, connectionId);
        
        log.info("Guacamole session created: username={}, connectionId={}", username, connectionId);
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
     * Connection ID로 iframe URL을 생성합니다.
     */
    public String buildIframeUrl(String connectionId) {
        String baseUrl = guacamoleConfig.getIframeBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "#/client/" + connectionId;
    }
}
