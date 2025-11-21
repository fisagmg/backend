package com.labhub.CveLabhubBack.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 관리자 웹 페이지에 반환되는 응답
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {

    private String alarmId;           // DynamoDB PK
    private String alarmName;         // cvexpert-HighMem-CVE-2025-29927
    private String instanceId;        // i-0364910353e1050cc
    private String metricName;        // MEMORY_USED
    private String namespace;         // cvexpert/CVE-2025-29927
    private String state;             // ALARM
    private String reason;            // Threshold Crossed...
    private String timestamp;         // 2025-11-21T11:00:00Z

    // AI 분석 결과
    private AnalysisInfo analysis;

    private String createdAt;         // MySQL 저장 시간
    private Boolean cached;           // 캐시된 결과인지 (현재는 항상 false)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisInfo {
        private String summary;
        private String severity;        // High, Medium, Low
        private String rootCause;
        private List<String> evidence;
        private List<String> recommendations;
    }
}