package com.labhub.CveLabhubBack.analysis.service;

import com.labhub.CveLabhubBack.analysis.dto.AnalysisRequest;
import com.labhub.CveLabhubBack.analysis.dto.AnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP 서버 호출 서비스
 * POST http://10.0.0.34:8000/api/analyze
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpService {

    private final WebClient mcpWebClient;

    @Value("${mcp.server.analyze-endpoint:/api/analyze}")
    private String analyzeEndpoint;

    /**
     * MCP 서버에 알람 분석 요청
     *
     * @param request 알람 정보
     * @return MCP 분석 결과
     */
    public AnalysisResult analyzeAlarm(AnalysisRequest request) {
        log.info("Calling MCP server for alarm analysis: {}", request.getAlarmName());

        try {
            // MCP 서버 요청 바디 생성
            Map<String, Object> requestBody = createMcpRequestBody(request);

            // MCP 서버 호출 (동기 방식)
            AnalysisResult result = mcpWebClient.post()
                    .uri(analyzeEndpoint)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(AnalysisResult.class)
                    .doOnError(error -> log.error("MCP server call failed: {}", error.getMessage()))
                    .onErrorResume(error -> {
                        // MCP 서버 호출 실패 시 기본 응답 반환
                        log.error("Failed to call MCP server, returning fallback response", error);
                        return Mono.just(createFallbackResponse(request, error.getMessage()));
                    })
                    .block(); // 동기로 변환 (필요시 비동기로 변경 가능)

            log.info("MCP server analysis completed: {}", result.getStatus());
            return result;

        } catch (Exception e) {
            log.error("Unexpected error during MCP server call", e);
            return createFallbackResponse(request, e.getMessage());
        }
    }

    /**
     * MCP 서버 요청 바디 생성
     */
    private Map<String, Object> createMcpRequestBody(AnalysisRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("alarm_name", request.getAlarmName());
        body.put("state", request.getState() != null ? request.getState() : "ALARM");
        body.put("instance_id", request.getInstanceId());
        body.put("metric_name", request.getMetricName() != null ? request.getMetricName() : "MEMORY_USED");
        body.put("namespace", request.getNamespace() != null ? request.getNamespace() : extractNamespace(request.getAlarmName()));
        body.put("reason", request.getReason() != null ? request.getReason() : "Threshold Crossed");
        body.put("timestamp", request.getTimestamp());

        return body;
    }

    /**
     * Alarm 이름에서 Namespace 추출
     * 예: cvexpert-HighMem-CVE-2025-29927 -> cvexpert/CVE-2025-29927
     */
    private String extractNamespace(String alarmName) {
        if (alarmName == null) return "cvexpert/unknown";

        // cvexpert-HighMem-CVE-2025-29927 형식에서 CVE-XXXX-XXXXX 추출
        String[] parts = alarmName.split("-");
        if (parts.length >= 4) {
            String cveId = String.join("-", parts[2], parts[3], parts[4]);
            return "cvexpert/" + cveId;
        }
        return "cvexpert/unknown";
    }

    /**
     * MCP 서버 호출 실패 시 Fallback 응답
     */
    private AnalysisResult createFallbackResponse(AnalysisRequest request, String errorMessage) {
        AnalysisResult.AnalysisDetail analysis = new AnalysisResult.AnalysisDetail();
        analysis.setSummary("MCP 서버 분석 실패");
        analysis.setSeverity("Unknown");
        analysis.setRootCause("MCP 서버와 통신할 수 없습니다: " + errorMessage);
        analysis.setEvidence(java.util.List.of("MCP 서버 호출 오류"));
        analysis.setRecommendations(java.util.List.of(
                "MCP 서버 상태 확인: http://10.0.0.34:8000/health",
                "네트워크 연결 확인",
                "MCP 서버 로그 확인"
        ));

        return AnalysisResult.builder()
                .status("error")
                .alarmName(request.getAlarmName())
                .instanceId(request.getInstanceId())
                .analysis(analysis)
                .notificationSent(false)
                .timestamp(request.getTimestamp())
                .build();
    }
}