//package com.labhub.CveLabhubBack.service;
//
//import com.labhub.CveLabhubBack.config.KeycloakFeignConfig;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.http.MediaType;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestParam;
//
///**
// * Keycloak 서버와 통신하여서 데이터를 수신합니다.
// */
//@FeignClient(
//        name = "keycloak-auth-service",
//        url = "http://localhost:8080/realms/dev-realm",
//        configuration = KeycloakFeignConfig.class // ✅ 여기가 연결 포인트
//)
//public interface AuthFlowService {
//
//    /**
//     * Direct Access Flow : 토큰을 즉시 요청하는 방법
//     *
//     * @return 토큰 값 반환
//     */
//
//
//    @PostMapping(
//            value = "/protocol/openid-connect/token",
//            consumes = "application/x-www-form-urlencoded"
//    )
//    Object getAccessToken(@RequestBody MultiValueMap<String, String> formData);
//}


package com.labhub.CveLabhubBack.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AuthFlowService {

    private static final String TOKEN_URL =
            "http://172.16.1.110:9090/realms/dev-realm/protocol/openid-connect/token";

    public Object getAccessToken(
            String grantType,
            String clientId,
            String clientSecret,
            String username,
            String password
    ) {

        // 1. form 바디 만들기
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", grantType);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", username);
        form.add("password", password);

        // 2. 헤더: x-www-form-urlencoded
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity =
                new HttpEntity<>(form, headers);

        // 3. 요청 날리기
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Object> response =
                restTemplate.postForEntity(TOKEN_URL, entity, Object.class);

        // 4. body만 돌려주자
        return response.getBody();
    }
}
