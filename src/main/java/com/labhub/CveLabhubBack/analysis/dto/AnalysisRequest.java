package com.labhub.CveLabhubBack.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Slack 버튼 클릭 시 전달되는 알람 정보
 * URL: GET /admin/analysis?alarm_name=xxx&instance_id=xxx&timestamp=xxx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {

    private String alarmName;      // cvexpert-HighMem-CVE-2025-29927
    private String instanceId;     // i-0364910353e1050cc
    private String timestamp;      // 2025-11-21T11:00:00Z
    private String metricName;     // MEMORY_USED (optional)
    private String namespace;      // cvexpert/CVE-2025-29927 (optional)
    private String state;          // ALARM (optional)
    private String reason;         // Threshold Crossed... (optional)
}