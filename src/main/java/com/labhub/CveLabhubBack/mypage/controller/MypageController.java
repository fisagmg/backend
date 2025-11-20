package com.labhub.CveLabhubBack.mypage.controller;

import com.labhub.CveLabhubBack.mypage.dto.PasswordChangeRequest;
import com.labhub.CveLabhubBack.mypage.dto.UserProfileResponse;
import com.labhub.CveLabhubBack.mypage.dto.UserUpdateRequest;
import com.labhub.CveLabhubBack.mypage.service.MypageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
@Tag(name = "Mypage API", description = "마이페이지 사용자 정보 관리 API")
public class MypageController {

    private final MypageService mypageService;

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 정보 조회", description = "로그인한 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/mypage/me - 조회 요청");
        UserProfileResponse response = mypageService.getCurrentUserProfile(jwt);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @Operation(summary = "사용자 정보 수정", description = "로그인한 사용자의 이름과 전화번호를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<UserProfileResponse> updateUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("PUT /api/mypage/me - 수정 요청: firstName={}, lastName={}, phone={}",
                request.getFirstName(), request.getLastName(), request.getPhone());
        
        UserProfileResponse response = mypageService.updateUserProfile(jwt, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/password")
    @Operation(summary = "비밀번호 변경", description = "로그인한 사용자의 비밀번호를 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (현재 비밀번호 불일치, 새 비밀번호 불일치 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PasswordChangeRequest request) {
        log.info("PUT /api/mypage/me/password - 비밀번호 변경 요청");
        
        mypageService.changePassword(jwt, request);
        return ResponseEntity.ok(Map.of("message", "비밀번호가 성공적으로 변경되었습니다."));
    }
}

