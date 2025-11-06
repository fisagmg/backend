package com.labhub.CveLabhubBack.report.controller;

import com.labhub.CveLabhubBack.report.dto.*;
import com.labhub.CveLabhubBack.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Profile("!test")
public class ReportController {

    private final ReportService reportService;

    /**
     * 1️⃣ 보고서 생성 (템플릿 복제)
     * POST /api/reports
     */
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(@RequestBody ReportCreateRequest request) {
        log.info("POST /api/reports - Creating report for userId={}, cveId={}", 
                request.getUserId(), request.getCveId());
        
        ReportResponse response = reportService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2️⃣ 보고서 업로드 (저장)
     * PUT /api/reports/{id}/file
     * 
     * @param id 보고서 ID
     * @param userId 사용자 ID (요청 파라미터 또는 인증 정보에서 추출)
     * @param file 업로드할 .docx 파일
     */
    @PutMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportUploadResponse> uploadReportFile(
            @PathVariable("id") Long id,
            @RequestParam("userId") Long userId,
            @RequestPart("file") MultipartFile file) {
        
        log.info("PUT /api/reports/{}/file - Uploading file for userId={}", id, userId);
        
        ReportUploadResponse response = reportService.uploadReportFile(id, userId, file);
        return ResponseEntity.ok(response);
    }

    /**
     * 3️⃣ 보고서 목록 조회
     * GET /api/reports/me?userId={userId}
     */
    @GetMapping("/me")
    public ResponseEntity<List<ReportResponse>> getMyReports(@RequestParam("userId") Long userId) {
        log.info("GET /api/reports/me - Fetching reports for userId={}", userId);
        
        List<ReportResponse> reports = reportService.getMyReports(userId);
        return ResponseEntity.ok(reports);
    }

    /**
     * 4️⃣ 보고서 다운로드
     * GET /api/reports/{id}/download
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<PresignedUrlResponse> downloadReport(
            @PathVariable("id") Long id,
            @RequestParam("userId") Long userId) {
        
        log.info("GET /api/reports/{}/download - Generating download URL for userId={}", id, userId);
        
        PresignedUrlResponse response = reportService.downloadReport(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 5️⃣ 보고서 삭제 (Soft Delete)
     * DELETE /api/reports/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteReport(
            @PathVariable("id") Long id,
            @RequestParam("userId") Long userId) {
        
        log.info("DELETE /api/reports/{} - Deleting report for userId={}", id, userId);
        
        reportService.deleteReport(id, userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "보고서가 성공적으로 삭제되었습니다.");
        response.put("reportId", id.toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 보고서 상세 조회
     * GET /api/reports/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getReportById(
            @PathVariable("id") Long id,
            @RequestParam("userId") Long userId) {
        
        log.info("GET /api/reports/{} - Fetching report detail for userId={}", id, userId);
        
        ReportResponse response = reportService.getReportById(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * CVE ID로 보고서 조회
     * GET /api/reports/cve/{cveId}
     */
    @GetMapping("/cve/{cveId}")
    public ResponseEntity<List<ReportResponse>> getReportsByCveId(
            @PathVariable("cveId") String cveId,
            @RequestParam("userId") Long userId) {
        
        log.info("GET /api/reports/cve/{} - Fetching reports for userId={}", cveId, userId);
        
        List<ReportResponse> reports = reportService.getReportsByCveId(cveId, userId);
        return ResponseEntity.ok(reports);
    }
}

