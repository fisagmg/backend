package com.labhub.CveLabhubBack.controller;

import com.labhub.CveLabhubBack.dto.AuthResponseWrapper;
import com.labhub.CveLabhubBack.dto.AuthSuccessData;
import com.labhub.CveLabhubBack.dto.KeycloakTokenResponse;
import com.labhub.CveLabhubBack.dto.LoginRequest;
import com.labhub.CveLabhubBack.service.AuthService;
import com.labhub.CveLabhubBack.service.InvalidCredentialException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin // 프론트 로컬에서 호출할 때 CORS 막히면 필요
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseWrapper<AuthSuccessData>> login(@Valid @RequestBody LoginRequest request) {
        System.out.println("출력출력출력출력");
        try {
            // 비즈니스 로직 실행
            AuthSuccessData data = authService.login(request);

            // 성공한 경우 → 200 + SUCCESS 포맷
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(AuthResponseWrapper.success(data));

        } catch (InvalidCredentialException e) {
            // 이메일 or 비번 틀림 → 401
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponseWrapper.error(
                            401,
                            "이메일 또는 비밀번호가 올바르지 않습니다."
                    ));
        } catch (Exception e) {
            // 예상 못한 서버 문제 → 500
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponseWrapper.error(
                            500,
                            "잠시 후 다시 시도해주세요."
                    ));
        }
    }
}
