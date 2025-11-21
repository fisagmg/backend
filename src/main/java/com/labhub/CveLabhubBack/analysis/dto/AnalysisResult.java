package com.labhub.CveLabhubBack.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP 서버로부터 받는 분석 결과
 * MCP Server Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {

    private String status;         // success or error
    private String alarmName;      // cvexpert-HighMem-CVE-2025-29927
    private String instanceId;     // i-0364910353e1050cc
    private AnalysisDetail analysis;
    private Boolean notificationSent;
    private String timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisDetail {
        private String summary;            // "메모리 사용률 임계값 초과"
        private String severity;           // "High", "Medium", "Low"
        private String rootCause;          // 근본 원인 분석
        private List<String> evidence;     // 로그 증거
        private List<String> recommendations; // 권장 조치
    }
}