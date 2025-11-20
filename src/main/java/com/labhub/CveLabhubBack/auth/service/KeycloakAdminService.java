package com.labhub.CveLabhubBack.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class KeycloakAdminService {

    @Value("${keycloak.base-url}")
    private String BASE_URL;

    @Value("${keycloak.realm}")
    private String REALM;

    @Value("${keycloak.client-id}")
    private String CLIENT_ID;

    @Value("${keycloak.client-secret}")
    private String CLIENT_SECRET;

    private final RestTemplate rt = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    public String getServiceToken() {
        String url = BASE_URL + "/realms/" + REALM + "/protocol/openid-connect/token";

        MultiValueMap<String,String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", CLIENT_ID);
        form.add("client_secret", CLIENT_SECRET);

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<String> res = rt.postForEntity(url, new HttpEntity<>(form, h), String.class);
            JsonNode root = om.readTree(res.getBody());
            return root.get("access_token").asText();
        } catch (HttpClientErrorException e) {
            // 401일 때 상세 에러 바디 확인용
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("service token 파싱 실패", e);
        }
    }

    public String createUser(String email, String password, String firstName, String lastName, String phone) {
        String token = getServiceToken();
        String url = BASE_URL + "/admin/realms/" + REALM + "/users";

        String payload = """
        {
          "enabled": true,
          "username": "%s",
          "email": "%s",
          "firstName": "%s",
          "lastName": "%s",
          "emailVerified": true,
          "credentials": [ { "type": "password", "value": "%s", "temporary": false } ],
          "attributes": { "phone": ["%s"] }
        }
        """.formatted(email, email, firstName, lastName, password, phone == null ? "" : phone);

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);

        try {
            ResponseEntity<Void> res = rt.postForEntity(url, new HttpEntity<>(payload, h), Void.class);
            if (res.getStatusCode().value() == 201) {
                String location = res.getHeaders().getFirst("Location");
                return location != null ? location.substring(location.lastIndexOf('/') + 1) : null;
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 409) throw new RuntimeException("이미 존재하는 사용자");
            throw e;
        }
        throw new RuntimeException("Keycloak 사용자 생성 실패");
    }

    public void sendVerifyEmail(String userId) {
        String url =  BASE_URL + "/admin/realms/" + REALM + "/users/" + userId + "/execute-actions-email";
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(getServiceToken());
        h.setContentType(MediaType.APPLICATION_JSON);
        rt.exchange(url, HttpMethod.PUT, new HttpEntity<>(List.of("VERIFY_EMAIL"), h), Void.class);
    }

    /**
     * Keycloak 사용자 정보 업데이트
     * email은 Keycloak에서 필수 필드이므로 반드시 포함해야 함
     */
    public void updateUser(String userId, String email, String firstName, String lastName, String phone) {
        String token = getServiceToken();
        String url = BASE_URL + "/admin/realms/" + REALM + "/users/" + userId;

        String payload = """
        {
          "email": "%s",
          "firstName": "%s",
          "lastName": "%s",
          "emailVerified": true,
          "attributes": { "phone": ["%s"] }
        }
        """.formatted(
                nullToEmpty(email),
                nullToEmpty(firstName),
                nullToEmpty(lastName),
                nullToEmpty(phone)
        );

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);

        try {
            rt.exchange(url, HttpMethod.PUT, new HttpEntity<>(payload, h), Void.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                throw new RuntimeException("Keycloak 사용자를 찾을 수 없습니다: " + userId);
            }
            throw new RuntimeException("Keycloak 사용자 정보 업데이트 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Keycloak 사용자 비밀번호 변경
     */
    public void changePassword(String userId, String newPassword) {
        String token = getServiceToken();
        String url = BASE_URL + "/admin/realms/" + REALM + "/users/" + userId + "/reset-password";

        String payload = """
        {
          "type": "password",
          "value": "%s",
          "temporary": false
        }
        """.formatted(newPassword);

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);

        try {
            rt.exchange(url, HttpMethod.PUT, new HttpEntity<>(payload, h), Void.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                throw new RuntimeException("Keycloak 사용자를 찾을 수 없습니다: " + userId);
            }
            throw new RuntimeException("Keycloak 비밀번호 변경 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 현재 비밀번호 검증 (로그인 시도로 검증)
     */
    public boolean verifyPassword(String email, String password) {
        try {
            String url = BASE_URL + "/realms/" + REALM + "/protocol/openid-connect/token";
            
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "password");
            form.add("client_id", CLIENT_ID);
            form.add("client_secret", CLIENT_SECRET);
            form.add("username", email);
            form.add("password", password);

            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            rt.postForEntity(url, new HttpEntity<>(form, h), String.class);
            return true; // 로그인 성공 = 비밀번호 일치
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                return false; // 인증 실패 = 비밀번호 불일치
            }
            throw new RuntimeException("비밀번호 검증 중 오류 발생: " + e.getMessage(), e);
        }
    }

    // 만약 null로 값이 들어오면 ""로 변경, JSON 구조 깨지는 위험 방지
    private String nullToEmpty(String v) { return v == null ? "" : v; }
}
