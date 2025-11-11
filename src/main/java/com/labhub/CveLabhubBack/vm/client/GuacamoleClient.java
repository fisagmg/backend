package com.labhub.CveLabhubBack.vm.client;

import com.labhub.CveLabhubBack.vm.config.GuacamoleConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GuacamoleClient {

    private final GuacamoleConfig guacamoleConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Guacamole 로그인 - 토큰 발급
     */
    public String login() {
        String url = guacamoleConfig.getBaseUrl() + "/api/tokens";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", guacamoleConfig.getUsername());
        body.add("password", guacamoleConfig.getPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String token = (String) response.getBody().get("authToken");
                log.info("Guacamole 로그인 성공. Token: {}", token.substring(0, 20) + "...");
                return token;
            }

            throw new RuntimeException("Guacamole 로그인 실패");

        } catch (Exception e) {
            log.error("Guacamole 로그인 에러: {}", e.getMessage());
            throw new RuntimeException("Guacamole 로그인 실패", e);
        }
    }

    /**
     * SSH 연결 생성
     */
    public String createConnection(String token, String connectionName, String hostname, String privateKey) {
        String url = guacamoleConfig.getBaseUrl() + "/api/session/data/mysql/connections?token=" + token;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> connectionData = Map.of(
                "name", connectionName,
                "parentIdentifier", "ROOT",
                "protocol", "ssh",
                "parameters", Map.of(
                        "hostname", hostname,
                        "port", "22",
                        "username", "ubuntu",
                        "private-key", privateKey
                ),
                "attributes", Map.of(
                        "max-connections", "",
                        "max-connections-per-user", ""
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(connectionData, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String connectionId = (String) response.getBody().get("identifier");
                log.info("Guacamole SSH 연결 생성 성공. Connection ID: {}", connectionId);
                return connectionId;
            }

            throw new RuntimeException("Guacamole SSH 연결 생성 실패");

        } catch (Exception e) {
            log.error("Guacamole SSH 연결 생성 에러: {}", e.getMessage());
            throw new RuntimeException("Guacamole SSH 연결 생성 실패", e);
        }
    }

    /**
     * SSH 연결 삭제
     */
    public void deleteConnection(String token, String connectionId) {
        String url = guacamoleConfig.getBaseUrl() + "/api/session/data/mysql/connections/" + connectionId + "?token=" + token;

        try {
            restTemplate.delete(url);
            log.info("Guacamole SSH 연결 삭제 성공. Connection ID: {}", connectionId);

        } catch (Exception e) {
            log.error("Guacamole SSH 연결 삭제 에러: {}", e.getMessage());
            throw new RuntimeException("Guacamole SSH 연결 삭제 실패", e);
        }
    }

    /**
     * 웹 터미널 URL 생성
     */
    public String getTerminalUrl(String token, String connectionId) {
        return guacamoleConfig.getBaseUrl() + "/#/client/" + connectionId + "?token=" + token;
    }
}