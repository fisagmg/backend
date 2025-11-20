package com.labhub.CveLabhubBack.cve_lab.controller;

import com.labhub.CveLabhubBack.cve_lab.dto.LabCreateRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.LabCreateResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.RunRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.RunResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabExtendableResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabExtendResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabRemainingTimeResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabTerminateResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabExtendResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabExtendableResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabRemainingTimeResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabTerminateResponse;
import com.labhub.CveLabhubBack.cve_lab.service.LabService;
import com.labhub.CveLabhubBack.cve_lab.service.LabSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;  // 추가



@RestController
@RequestMapping("/api/labs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lab Session API", description = "실습 세션 생성 및 시간 관리 API")
public class LabController {
    private final LabService labService;
    private final LabSessionService labSessionService;

    @PostMapping("/create")
    @Operation(summary = "실습 환경 생성", description = "새로운 Lab 실습 환경을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "실습 환경 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<LabCreateResponse> create(@RequestBody LabCreateRequest req,
                                                @AuthenticationPrincipal Jwt jwt) {
    if (jwt != null) {
        log.info("JWT claims: {}", jwt.getClaims());
        log.info("preferred_username: {}", jwt.getClaimAsString("preferred_username"));
        log.info("email: {}", jwt.getClaimAsString("email"));
        log.info("sub: {}", jwt.getClaimAsString("sub"));
    }
    
    String userId = (jwt != null && jwt.hasClaim("sub"))
            ? jwt.getClaimAsString("sub")
            : null;
    
    // preferred_username 추출 (email 또는 username)
    String preferredUsername = (jwt != null && jwt.hasClaim("preferred_username"))
            ? jwt.getClaimAsString("preferred_username")
            : (jwt != null && jwt.hasClaim("email"))
                ? jwt.getClaimAsString("email")
                : null;
    
    LabCreateResponse response = labService.create(userId, preferredUsername, req);
    return ResponseEntity.ok(response);
    }

    @PostMapping("/destroy")
    @Operation(summary = "실습 환경 삭제", description = "기존 Lab 실습 환경을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "실습 환경 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "Lab 세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<RunResponse> destroy(@RequestBody RunRequest req,
                                               @AuthenticationPrincipal Jwt jwt) {
        String userId = (jwt != null && jwt.hasClaim("sub"))
                ? jwt.getClaimAsString("sub")
                : null;

        RunResponse response = labService.destroy(userId, req);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uuid}/remaining-time")
    @Operation(summary = "실습 잔여시간 조회", description = "Lab 세션의 남은 시간을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "Lab 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "이미 종료된 세션")
    })
    public ResponseEntity<LabRemainingTimeResponse> getRemainingTime(
            @Parameter(description = "Lab 세션 UUID", required = true)
            @PathVariable String uuid) {
        LabRemainingTimeResponse response = labSessionService.getRemainingTime(uuid);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uuid}/extendable")
    @Operation(summary = "시간 연장 가능 여부 확인", description = "Lab 세션의 시간 연장 가능 여부를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "Lab 세션을 찾을 수 없음")
    })
    public ResponseEntity<LabExtendableResponse> checkExtendable(
            @Parameter(description = "Lab 세션 UUID", required = true)
            @PathVariable String uuid) {
        LabExtendableResponse response = labSessionService.isExtendable(uuid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{uuid}/extend")
    @Operation(summary = "실습 시간 연장", description = "Lab 세션의 시간을 연장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "연장 성공"),
        @ApiResponse(responseCode = "400", description = "연장 불가 (최대 시간 초과)"),
        @ApiResponse(responseCode = "404", description = "Lab 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "이미 종료된 세션")
    })
    public ResponseEntity<LabExtendResponse> extendSession(
            @Parameter(description = "Lab 세션 UUID", required = true)
            @PathVariable String uuid) {
        LabExtendResponse response = labSessionService.extendLabSession(uuid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{uuid}/terminate")
    @Operation(summary = "VM 종료", description = "VM만 종료하고 LabStatus는 ACTIVE로 유지합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "VM 종료 성공"),
        @ApiResponse(responseCode = "404", description = "Lab 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "이미 완료되거나 취소된 세션"),
        @ApiResponse(responseCode = "500", description = "AWS EC2 종료 실패")
    })
    public ResponseEntity<LabTerminateResponse> terminateSession(
            @Parameter(description = "Lab 세션 UUID", required = true)
            @PathVariable String uuid) {
        LabTerminateResponse response = labSessionService.terminateLabSession(uuid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{uuid}/complete")
    @Operation(summary = "실습 완료", description = "실습을 완료하고 VM을 종료합니다. LabStatus는 COMPLETED로 변경됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "실습 완료 성공"),
        @ApiResponse(responseCode = "404", description = "Lab 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "ACTIVE 상태가 아닌 세션"),
        @ApiResponse(responseCode = "500", description = "AWS EC2 종료 실패")
    })
    public ResponseEntity<LabTerminateResponse> completeSession(
            @Parameter(description = "Lab 세션 UUID", required = true)
            @PathVariable String uuid) {
        LabTerminateResponse response = labSessionService.completeLabSession(uuid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{uuid}/cancel")
    @Operation(summary = "실습 취소", description = "실습을 취소하고 VM을 종료합니다. LabStatus는 CANCELLED로 변경됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "실습 취소 성공"),
        @ApiResponse(responseCode = "404", description = "Lab 세션을 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "ACTIVE 또는 CREATED 상태가 아닌 세션"),
        @ApiResponse(responseCode = "500", description = "AWS EC2 종료 실패")
    })
    public ResponseEntity<LabTerminateResponse> cancelSession(
            @Parameter(description = "Lab 세션 UUID", required = true)
            @PathVariable String uuid) {
        LabTerminateResponse response = labSessionService.cancelLabSession(uuid);
        return ResponseEntity.ok(response);
    }
}
