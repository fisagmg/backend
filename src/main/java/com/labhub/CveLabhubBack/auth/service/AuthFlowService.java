package com.labhub.CveLabhubBack.auth.service;

import com.labhub.CveLabhubBack.auth.Repository.UserRepository;
import com.labhub.CveLabhubBack.auth.dto.RegisterRequestDto;
import com.labhub.CveLabhubBack.auth.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFlowService {

    private final KeycloakAdminService keycloakAdminService;
    private final UserRepository userRepository;

    @Value("${keycloak.base-url}")
    private String keycloakBaseUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.grant-type}")
    private String grantType;

    public Object getAccessToken(
            String grantType,
            String clientId,
            String clientSecret,
            String username,
            String password
    ) {

        String tokenUrl = keycloakBaseUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";

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
                restTemplate.postForEntity(tokenUrl, entity, Object.class);

        // 4. body만 돌려주기
        return response.getBody();
    }

    @Transactional
    public String signup(RegisterRequestDto request) {
        log.info("[SIGNUP] 회원가입 시도: email={}, firstName={}, lastName={}", 
                request.getEmail(), request.getFirstName(), request.getLastName());

        // 1. Keycloak에 사용자 생성
        String kcUserId = keycloakAdminService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhone()
        );

        // 2. 인증 이메일 전송
        keycloakAdminService.sendVerifyEmail(kcUserId);

        // 3. DB에 사용자 정보 저장
        UserEntity user = new UserEntity();
        user.setKcUserId(kcUserId);
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        userRepository.save(user);

        log.info("[SIGNUP] 회원가입 성공: userId={}, email={}", kcUserId, request.getEmail());
        return kcUserId;
    }

    public Object login(String username, String password) {
        return getAccessToken(grantType, clientId, clientSecret, username, password);
    }
}
