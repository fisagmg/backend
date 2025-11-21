package com.labhub.CveLabhubBack.analysis.controller;

import com.labhub.CveLabhubBack.analysis.dto.AnalysisRequest;
import com.labhub.CveLabhubBack.analysis.dto.AnalysisResponse;
import com.labhub.CveLabhubBack.analysis.dto.AnalysisResult;
import com.labhub.CveLabhubBack.analysis.service.AnalysisService;
import com.labhub.CveLabhubBack.analysis.service.McpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 알람 분석 API (MySQL 버전)
 *
 * Endpoint: GET/POST /admin/analysis
 *
 * Flow:
 * 1. Slack 버튼 클릭 → URL: http://172.16.3.20:8082/admin/analysis?alarm_name=xxx&instance_id=xxx&timestamp=xxx
 * 2. MCP 서버 호출 (http://10.0.0.34:8000/api/analyze)
 * 3. Bedrock 분석 결과 받기
 * 4. MySQL에 저장
 * 5. 결과 반환 (JSON)
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AnalysisController {

    private final McpService mcpService;
    private final AnalysisService analysisService;

    /**
     * GET 방식: Slack 버튼에서 쿼리 파라미터로 호출
     *
     * @param alarmName   알람 이름 (예: cvexpert-HighMem-CVE-2025-29927)
     * @param instanceId  EC2 인스턴스 ID (예: i-0364910353e1050cc)
     * @param timestamp   알람 발생 시간 (ISO 8601 형식)
     * @return AnalysisResponse
     */
    @GetMapping("/analysis")
    public ResponseEntity<?> analyzeAlarmGet(
            @RequestParam("alarm_name") String alarmName,
            @RequestParam("instance_id") String instanceId,
            @RequestParam("timestamp") String timestamp,
            @RequestParam(value = "metric_name", required = false) String metricName,
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        log.info("=== Alarm Analysis Request (GET) ===");
        log.info("Alarm Name: {}", alarmName);
        log.info("Instance ID: {}", instanceId);
        log.info("Timestamp: {}", timestamp);

        try {
            // Request DTO 생성
            AnalysisRequest request = AnalysisRequest.builder()
                    .alarmName(alarmName)
                    .instanceId(instanceId)
                    .timestamp(timestamp)
                    .metricName(metricName)
                    .namespace(namespace)
                    .state(state)
                    .reason(reason)
                    .build();

            // 분석 수행
            AnalysisResponse response = performAnalysis(request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to analyze alarm", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("분석 실패: " + e.getMessage()));
        }
    }

    /**
     * POST 방식: JSON 바디로 호출 (선택적)
     *
     * @param request AnalysisRequest
     * @return AnalysisResponse
     */
    @PostMapping("/analysis")
    public ResponseEntity<?> analyzeAlarmPost(@RequestBody AnalysisRequest request) {
        log.info("=== Alarm Analysis Request (POST) ===");
        log.info("Request: {}", request);

        try {
            AnalysisResponse response = performAnalysis(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to analyze alarm", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("분석 실패: " + e.getMessage()));
        }
    }

    /**
     * 실제 분석 수행 로직
     * 1. MCP 서버 호출
     * 2. MySQL 저장
     * 3. 응답 반환
     */
    private AnalysisResponse performAnalysis(AnalysisRequest request) {
        log.info("Step 1: Calling MCP server...");

        // 1. MCP 서버 호출 (Bedrock 분석)
        AnalysisResult mcpResult = mcpService.analyzeAlarm(request);

        if ("error".equals(mcpResult.getStatus())) {
            log.warn("MCP server returned error status");
        }

        log.info("Step 2: Saving to MySQL...");

        // 2. MySQL에 저장
        AnalysisResponse response = analysisService.saveAnalysisResult(request, mcpResult);

        log.info("Step 3: Analysis completed successfully");
        log.info("Alarm ID: {}", response.getAlarmId());
        log.info("Severity: {}", response.getAnalysis() != null ? response.getAnalysis().getSeverity() : "N/A");

        return response;
    }

    /**
     * alarm_id로 조회 (선택적 기능)
     */
    @GetMapping("/analysis/{alarmId}")
    public ResponseEntity<?> getAnalysisById(@PathVariable String alarmId) {
        log.info("Getting analysis by alarm_id: {}", alarmId);

        try {
            AnalysisResponse response = analysisService.getByAlarmId(alarmId);

            if (response == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("알람 분석 결과를 찾을 수 없습니다: " + alarmId));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get analysis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 최근 분석 목록 조회 (대시보드용)
     */
    @GetMapping("/analysis/recent")
    public ResponseEntity<?> getRecentAnalyses() {
        log.info("Getting recent analyses");

        try {
            List<AnalysisResponse> responses = analysisService.getRecentAnalyses();
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            log.error("Failed to get recent analyses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 에러 응답 생성
     */
    private Object createErrorResponse(String message) {
        return new ErrorResponse(message);
    }

    /**
     * 에러 응답 DTO
     */
    public record ErrorResponse(String error) {}

    /**
     * 헬스체크 엔드포인트
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Admin Analysis Service is running (MySQL version)");
    }
}