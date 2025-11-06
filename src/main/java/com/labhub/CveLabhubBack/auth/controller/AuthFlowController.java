package com.labhub.CveLabhubBack.auth.controller;

import com.labhub.CveLabhubBack.auth.dto.LoginRequestDto;
import com.labhub.CveLabhubBack.auth.dto.RegisterRequestDto;
import com.labhub.CveLabhubBack.auth.entity.UserEntity;
import com.labhub.CveLabhubBack.auth.Repository.UserRepository;
import com.labhub.CveLabhubBack.auth.service.AuthFlowService;
import com.labhub.CveLabhubBack.auth.service.EmailService;
import com.labhub.CveLabhubBack.auth.service.KeycloakAdminService;
import com.labhub.CveLabhubBack.auth.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AuthFlowController {

    private final AuthFlowService authFlowService; // 토큰 발급 담당
    private final KeycloakAdminService keycloakAdminService; // 유저 생성 담당
    private final OtpService otpService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${keycloak.client-id}")
    private String CLIENT_ID;

    @Value("${keycloak.client-secret}")
    private String CLIENT_SECRET;

    @Value("${keycloak.grant-type}")
    private String GRANT_TYPE;

    /**
     * Direct Access Grants Flow : 토큰을 즉시 요청하는 방법
     * @return Keycloak에서 발급한 토큰 값 반환
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
        try {
            log.info("[LOGIN] going to Keycloak with username={}, password={}",
                    request.getUsername(), request.getPassword());

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", GRANT_TYPE);
            form.add("client_id", CLIENT_ID);
            form.add("client_secret", CLIENT_SECRET);
            form.add("username", request.getUsername());
            form.add("password", request.getPassword());

            log.debug("[LOGIN-DEBUG] Sending to Keycloak: "
                            + "grant_type={}, client_id={}, client_secret=****, username={}, password=****",
                    GRANT_TYPE, CLIENT_ID, request.getUsername());

            Object tokenResponse = authFlowService.getAccessToken(
                    GRANT_TYPE,
                    CLIENT_ID,
                    CLIENT_SECRET,
                    request.getUsername(),
                    request.getPassword()
            );

            log.info("[LOGIN] Keycloak response = {}", tokenResponse);
            return ResponseEntity.ok(tokenResponse);

        } catch (Exception e) {
            log.error("[LOGIN] Keycloak rejected login", e);
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    /** 회원가입 → Keycloak에 사용자 생성 */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody RegisterRequestDto req) {
        if (req.getEmail() == null || req.getPassword() == null || 
            req.getFirstName() == null || req.getLastName() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "email, password, firstName, lastName는 필수입니다."
            ));
        }

        try {
            log.info("[SIGNUP] 회원가입 시도: email={}, firstName={}, lastName={}", 
                    req.getEmail(), req.getFirstName(), req.getLastName());
            
            String userId = keycloakAdminService.createUser(
                    req.getEmail(),
                    req.getPassword(),
                    req.getFirstName(),
                    req.getLastName(),
                    req.getPhone()
            );

            keycloakAdminService.sendVerifyEmail(userId);

            // DB 저장
            UserEntity user = new UserEntity();
            user.setKcUserId(userId);
            user.setEmail(req.getEmail());
            user.setFirstName(req.getFirstName());
            user.setLastName(req.getLastName());
            user.setPhone(req.getPhone());
            userRepository.save(user);

            log.info("[SIGNUP] 회원가입 성공: userId={}, email={}", userId, req.getEmail());
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "userId", userId,
                    "email", req.getEmail(),
                    "verification", "MAIL_SENT"
            ));


        } catch (HttpClientErrorException e) {
            log.error("[SIGNUP] HttpClientErrorException: status={}, body={}", 
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (e.getStatusCode().value() == 409) {
                return ResponseEntity.status(409).body(Map.of(
                        "status", "ERROR",
                        "message", "이미 존재하는 사용자입니다."
                ));
            }
            return ResponseEntity.status(502).body(Map.of(
                    "status", "ERROR",
                    "message", "인증 서버 오류: " + e.getStatusCode() + " - " + e.getResponseBodyAsString()
            ));
        } catch (Exception e) {
            log.error("[SIGNUP] 회원가입 처리 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "회원가입 처리 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
    // com.labhub.CveLabhubBack.auth.controller.AuthFlowController (기존 클래스에 추가)
    @PostMapping("/otp/send")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        // 회사 도메인 검증은 프론트/백 모두에서 하는 것을 권장
        String code = otpService.generate(email);
        emailService.sendOtp(email, code);
        return ResponseEntity.ok(Map.of("status","SENT"));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String code) {
        boolean ok = otpService.verify(email, code);
        if (!ok) return ResponseEntity.status(400).body(Map.of("status","INVALID"));
        otpService.consume(email); // 1회성 소모
        return ResponseEntity.ok(Map.of("status","OK"));
    }
}
