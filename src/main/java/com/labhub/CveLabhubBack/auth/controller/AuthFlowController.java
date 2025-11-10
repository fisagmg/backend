package com.labhub.CveLabhubBack.auth.controller;

import com.labhub.CveLabhubBack.auth.dto.LoginRequestDto;
import com.labhub.CveLabhubBack.auth.dto.RegisterRequestDto;
import com.labhub.CveLabhubBack.auth.service.AuthFlowService;
import com.labhub.CveLabhubBack.auth.service.EmailService;
import com.labhub.CveLabhubBack.auth.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:3000")
public class AuthFlowController {

    private final AuthFlowService authFlowService;
    private final OtpService otpService;
    private final EmailService emailService;

    /**
     * 로그인 - Keycloak 토큰 발급
     * @return Keycloak에서 발급한 토큰 값 반환
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
        try {
            log.info("[LOGIN] going to Keycloak with username={}, password={}",
                    request.getUsername(), request.getPassword());

            Object tokenResponse = authFlowService.login(
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

    /**
     * 회원가입 - Keycloak 사용자 생성 + DB 저장
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody RegisterRequestDto req) {
        // 입력값 검증
        if (req.getEmail() == null || req.getPassword() == null || 
            req.getFirstName() == null || req.getLastName() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "email, password, firstName, lastName는 필수입니다."
            ));
        }

        try {
            // Service에서 회원가입 처리 (Keycloak 생성 + DB 저장)
            String userId = authFlowService.signup(req);

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
