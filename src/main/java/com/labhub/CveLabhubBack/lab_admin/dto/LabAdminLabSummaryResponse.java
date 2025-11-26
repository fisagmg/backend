package com.labhub.CveLabhubBack.lab_admin.dto;

import java.time.LocalDateTime;

public record LabAdminLabSummaryResponse(
        String labUuid,
        Integer cveId,
        String cveName,
        String userEmail,
        String userDisplayName,
        String instanceId,
        String status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        Long ttlRemainingSeconds,
        Long ttlRemainingMinutes,
        boolean monitoringAvailable
) {
}


