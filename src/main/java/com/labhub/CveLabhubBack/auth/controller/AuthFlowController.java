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
public class AuthFlowController {

    private final AuthFlowService authFlowService;
    private final OtpService otpService;
    private final EmailService emailService;

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

    // 이메일 인증번호 보내기
    @PostMapping("/otp/send")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        String code = otpService.generate(email); // 랜덤 코드 생성
        emailService.sendOtp(email, code); //실제 이메일 전송
        return ResponseEntity.ok(Map.of("status","SENT"));
    }

    // 인증번호 검증
    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String code) {
        boolean ok = otpService.verify(email, code);
        if (!ok) return ResponseEntity.status(400).body(Map.of("status","INVALID"));
        otpService.consume(email); // 인증 성공 시 저장된 인증코드 데이터 삭제
        return ResponseEntity.ok(Map.of("status","OK"));
    }
}
