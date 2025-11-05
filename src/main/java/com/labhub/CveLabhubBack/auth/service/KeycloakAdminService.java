package com.labhub.CveLabhubBack.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class KeycloakAdminService {

    // Keycloak 주소와 realm
    private static final String BASE_URL  = "http://172.16.1.110:9090"; // 실제 Keycloak 접속 주소/포트
    private static final String REALM     = "dev-realm";                // 토큰 받는 곳 = 유저 생성할 realm

    // 서비스 계정(Confidential Client)
    private static final String CLIENT_ID     = "labhub-admin";
    private static final String CLIENT_SECRET = "fTjPQl0mkwkUi3qehOdprRiSjZlRP53Y";
    // ==========================================

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
}
