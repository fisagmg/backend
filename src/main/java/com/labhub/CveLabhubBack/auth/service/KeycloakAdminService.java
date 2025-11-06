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

    @Value("${keycloak.base-url}")      // 예: http://172.16.1.110:9090
    private String BASE_URL;

    @Value("${keycloak.realm}")         // 예: dev-realm
    private String REALM;

    @Value("${keycloak.client-id}")     // 예: labhub-admin
    private String CLIENT_ID;

    @Value("${keycloak.client-secret}") // 예: xxxxxx (앱 비밀번호 아님!)
    private String CLIENT_SECRET;

    private final RestTemplate rt = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    /** 서비스 계정 토큰 */
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
            System.err.println("TOKEN ERROR " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("service token 파싱 실패", e);
        }
    }

    /** 유저 생성 */
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
            System.err.println("CREATE USER ERROR " + e.getStatusCode() + " / " + e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 409) throw new RuntimeException("이미 존재하는 사용자");
            throw e;
        }
        throw new RuntimeException("Keycloak 사용자 생성 실패");
    }

    /** 이메일로 userId 찾기(정확 매칭) */
    public String findUserIdByEmail(String email) {
        String url = BASE_URL + "/admin/realms/" + REALM + "/users?email=" + email + "&exact=true";
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(getServiceToken());
        ResponseEntity<List> resp = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(h), List.class);
        if (resp.getBody() == null || resp.getBody().isEmpty()) {
            throw new IllegalArgumentException("No user found by email: " + email);
        }
        Map first = (Map) resp.getBody().get(0);
        return String.valueOf(first.get("id"));
    }

    /** 인증메일 보내기 (VERIFY_EMAIL 액션) */
    public void sendVerifyEmail(String userId) {
        String url =  BASE_URL + "/admin/realms/" + REALM + "/users/" + userId + "/execute-actions-email";
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(getServiceToken());
        h.setContentType(MediaType.APPLICATION_JSON);
        rt.exchange(url, HttpMethod.PUT, new HttpEntity<>(List.of("VERIFY_EMAIL"), h), Void.class);
    }

    /** emailVerified 강제 토글(테스트/리셋용) */
    public void markEmailVerified(String userId, boolean verified) {
        String url = BASE_URL + "/admin/realms/" + REALM + "/users/" + userId;
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(getServiceToken());
        h.setContentType(MediaType.APPLICATION_JSON);
        rt.exchange(url, HttpMethod.PUT, new HttpEntity<>(Map.of("emailVerified", verified), h), Void.class);
    }

    private String nullToEmpty(String v) { return v == null ? "" : v; }
}
