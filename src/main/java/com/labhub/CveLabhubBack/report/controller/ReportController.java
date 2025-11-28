package com.labhub.CveLabhubBack.report.controller;

import com.labhub.CveLabhubBack.auth.Repository.UserRepository;
import com.labhub.CveLabhubBack.report.dto.*;
import com.labhub.CveLabhubBack.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Report", description = "CVE 보고서 생성, 업로드, 조회, 삭제 API")
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Profile("!test")
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    /**
     * JWT에서 userId 추출
     */
    private Long getUserIdFromJwt(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }
        
        final String finalEmail = email;
        if (finalEmail == null || finalEmail.isBlank()) {
            throw new IllegalStateException("JWT에서 이메일을 찾을 수 없습니다.");
        }
        
        return userRepository.findByEmail(finalEmail)
                .orElseThrow(() -> new IllegalStateException("등록되지 않은 사용자: " + finalEmail))
                .getId();
    }

    @Operation(summary = "보고서 생성", description = "템플릿을 복제하여 새로운 보고서를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "보고서 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @RequestBody ReportCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long userId = getUserIdFromJwt(jwt);
        //log.info("POST /api/reports - Creating report for userId={}, cveId={}", 
                userId, request.getCveId());
        
        //log.debug("📥 [REPORT CREATE REQUEST] userId={}, cveId={}, name={}", 
                userId, request.getCveId(), request.getName());
        
        ReportResponse response = reportService.createReport(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "보고서 파일 업로드", description = "작성한 보고서 파일(.docx)을 S3에 업로드합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 파일 형식 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "보고서를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportUploadResponse> uploadReportFile(
            @Parameter(description = "보고서 ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "업로드할 .docx 파일", required = true) @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long userId = getUserIdFromJwt(jwt);
        //log.info("PUT /api/reports/{}/file - Uploading file for userId={}", id, userId);
        
        ReportUploadResponse response = reportService.uploadReportFile(id, userId, file);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 보고서 목록 조회", description = "로그인한 사용자가 작성한 모든 보고서 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/me")
    public ResponseEntity<List<ReportResponse>> getMyReports(
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);
        //log.info("GET /api/reports/me - Fetching reports for userId={}", userId);
        
        List<ReportResponse> reports = reportService.getMyReports(userId);
        return ResponseEntity.ok(reports);
    }

    @Operation(summary = "보고서 다운로드 URL 생성", description = "S3에 저장된 보고서를 다운로드할 수 있는 presigned URL을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "보고서를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{id}/download")
    public ResponseEntity<PresignedUrlResponse> downloadReport(
            @Parameter(description = "보고서 ID", required = true) @PathVariable("id") Long id,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long userId = getUserIdFromJwt(jwt);
        //log.info("GET /api/reports/{}/download - Generating download URL for userId={}", id, userId);
        
        PresignedUrlResponse response = reportService.downloadReport(id, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "보고서 삭제", description = "보고서를 논리적으로 삭제합니다 (Soft Delete).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "보고서를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteReport(
            @Parameter(description = "보고서 ID", required = true) @PathVariable("id") Long id,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long userId = getUserIdFromJwt(jwt);
        //log.info("DELETE /api/reports/{} - Deleting report for userId={}", id, userId);
        
        reportService.deleteReport(id, userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "보고서가 성공적으로 삭제되었습니다.");
        response.put("reportId", id.toString());
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "보고서 상세 조회", description = "특정 보고서의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "보고서를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getReportById(
            @Parameter(description = "보고서 ID", required = true) @PathVariable("id") Long id,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long userId = getUserIdFromJwt(jwt);
        //log.info("GET /api/reports/{} - Fetching report detail for userId={}", id, userId);
        
        ReportResponse response = reportService.getReportById(id, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "CVE ID로 보고서 조회", description = "특정 CVE ID와 관련된 모든 보고서를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/cve/{cveId}")
    public ResponseEntity<List<ReportResponse>> getReportsByCveId(
            @Parameter(description = "CVE ID (예: CVE-2024-1234)", required = true) @PathVariable("cveId") String cveId,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long userId = getUserIdFromJwt(jwt);
        //log.info("GET /api/reports/cve/{} - Fetching reports for userId={}", cveId, userId);
        
        List<ReportResponse> reports = reportService.getReportsByCveId(cveId, userId);
        return ResponseEntity.ok(reports);
    }
}

