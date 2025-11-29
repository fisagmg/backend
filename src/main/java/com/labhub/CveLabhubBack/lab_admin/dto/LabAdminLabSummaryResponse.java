package com.labhub.CveLabhubBack.lab_admin.dto;

import java.time.Instant;

public record LabAdminLabSummaryResponse(
        String labUuid,
        Integer cveId,
        String cveName,
        String userEmail,
        String userDisplayName,
        String instanceId,
        String status,
        Instant createdAt,
        Instant expiresAt,
        Long ttlRemainingSeconds,
        Long ttlRemainingMinutes,
        boolean monitoringAvailable
) {
}


