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

    private volatile String cachedToken;
    private volatile long tokenIssuedAt;
    private static final long TOKEN_TTL_MILLIS = 10 * 60 * 1000L; // 10분

    public synchronized String getAdminToken() {
        if (cachedToken != null && !isExpired()) {
            return cachedToken;
        }

        String url = guacamoleConfig.getBaseUrl() + "/api/tokens";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", guacamoleConfig.getAdminUsername());
        form.add("password", guacamoleConfig.getAdminPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, new HttpEntity<>(form, headers), Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to obtain Guacamole admin token");
        }

        cachedToken = (String) response.getBody().get("authToken");
        tokenIssuedAt = System.currentTimeMillis();
        return cachedToken;
    }

    private boolean isExpired() {
        return System.currentTimeMillis() - tokenIssuedAt > TOKEN_TTL_MILLIS;
    }
}