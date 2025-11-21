package com.labhub.CveLabhubBack.analysis.service;

import com.labhub.CveLabhubBack.analysis.dto.AnalysisRequest;
import com.labhub.CveLabhubBack.analysis.dto.AnalysisResponse;
import com.labhub.CveLabhubBack.analysis.dto.AnalysisResult;
import com.labhub.CveLabhubBack.analysis.entity.Analysis;
import com.labhub.CveLabhubBack.analysis.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * MySQL 기반 알람 분석 저장 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository repository;

    /**
     * 분석 결과를 MySQL에 저장
     *
     * @param request 원본 알람 요청
     * @param mcpResult MCP 분석 결과
     * @return AnalysisResponse (저장된 데이터)
     */
    @Transactional
    public AnalysisResponse saveAnalysisResult(AnalysisRequest request, AnalysisResult mcpResult) {
        log.info("Saving analysis result to MySQL: {}", request.getAlarmName());

        try {
            // alarm_id 생성 (PK): alarmName + timestamp의 epoch 값
            String alarmId = generateAlarmId(request.getAlarmName(), request.getTimestamp());

            // 이미 존재하는지 확인 (중복 방지)
            if (repository.existsByAlarmId(alarmId)) {
                log.warn("Analysis already exists for alarm_id: {}, returning existing data", alarmId);
                return getByAlarmId(alarmId);
            }

            // Entity 생성
            Analysis entity = Analysis.builder()
                    .alarmId(alarmId)
                    .alarmName(request.getAlarmName())
                    .instanceId(request.getInstanceId())
                    .metricName(request.getMetricName() != null ? request.getMetricName() : "MEMORY_USED")
                    .namespace(request.getNamespace() != null ? request.getNamespace() : extractNamespace(request.getAlarmName()))
                    .state(request.getState() != null ? request.getState() : "ALARM")
                    .reason(request.getReason() != null ? request.getReason() : "Threshold Crossed")
                    .alarmTimestamp(parseTimestamp(request.getTimestamp()))
                    .build();

            // AI 분석 결과 저장
            if (mcpResult.getAnalysis() != null) {
                entity.setAnalysisSummary(mcpResult.getAnalysis().getSummary());
                entity.setAnalysisSeverity(mcpResult.getAnalysis().getSeverity());
                entity.setAnalysisRootCause(mcpResult.getAnalysis().getRootCause());
                entity.setAnalysisEvidence(mcpResult.getAnalysis().getEvidence());
                entity.setAnalysisRecommendations(mcpResult.getAnalysis().getRecommendations());
            }

            // MySQL에 저장
            Analysis saved = repository.save(entity);
            log.info("Successfully saved to MySQL with ID: {}, alarm_id: {}", saved.getId(), saved.getAlarmId());

            // Response 생성
            return convertToResponse(saved);

        } catch (Exception e) {
            log.error("Failed to save to MySQL", e);
            throw new RuntimeException("MySQL 저장 실패: " + e.getMessage(), e);
        }
    }

    /**
     * alarm_id로 조회
     */
    @Transactional(readOnly = true)
    public AnalysisResponse getByAlarmId(String alarmId) {
        return repository.findByAlarmId(alarmId)
                .map(this::convertToResponse)
                .orElse(null);
    }

    /**
     * 최근 10개 조회
     */
    @Transactional(readOnly = true)
    public java.util.List<AnalysisResponse> getRecentAnalyses() {
        return repository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * alarm_id 생성: alarmName-timestamp의 epoch 값
     * 예: cvexpert-HighMem-CVE-2025-29927-1732156800
     */
    private String generateAlarmId(String alarmName, String timestamp) {
        try {
            long epochSecond = Instant.parse(timestamp).getEpochSecond();
            return alarmName + "-" + epochSecond;
        } catch (Exception e) {
            // timestamp 파싱 실패 시 현재 시간 사용
            return alarmName + "-" + Instant.now().getEpochSecond();
        }
    }

    /**
     * ISO 8601 문자열을 LocalDateTime으로 변환
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            return LocalDateTime.ofInstant(Instant.parse(timestamp), ZoneId.of("Asia/Seoul"));
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}, using current time", timestamp);
            return LocalDateTime.now();
        }
    }

    /**
     * Alarm 이름에서 Namespace 추출
     */
    private String extractNamespace(String alarmName) {
        if (alarmName == null) return "cvexpert/unknown";

        String[] parts = alarmName.split("-");
        if (parts.length >= 4) {
            String cveId = String.join("-", parts[2], parts[3], parts[4]);
            return "cvexpert/" + cveId;
        }
        return "cvexpert/unknown";
    }

    /**
     * Entity를 Response DTO로 변환
     */
    private AnalysisResponse convertToResponse(Analysis entity) {
        AnalysisResponse.AnalysisInfo analysisInfo = null;

        if (entity.getAnalysisSummary() != null) {
            analysisInfo = AnalysisResponse.AnalysisInfo.builder()
                    .summary(entity.getAnalysisSummary())
                    .severity(entity.getAnalysisSeverity())
                    .rootCause(entity.getAnalysisRootCause())
                    .evidence(entity.getAnalysisEvidence())
                    .recommendations(entity.getAnalysisRecommendations())
                    .build();
        }

        return AnalysisResponse.builder()
                .alarmId(entity.getAlarmId())
                .alarmName(entity.getAlarmName())
                .instanceId(entity.getInstanceId())
                .metricName(entity.getMetricName())
                .namespace(entity.getNamespace())
                .state(entity.getState())
                .reason(entity.getReason())
                .timestamp(entity.getAlarmTimestamp().atZone(ZoneId.of("Asia/Seoul"))
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .analysis(analysisInfo)
                .createdAt(entity.getCreatedAt().atZone(ZoneId.of("Asia/Seoul"))
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .cached(false) // 현재는 항상 실시간 분석
                .build();
    }
}