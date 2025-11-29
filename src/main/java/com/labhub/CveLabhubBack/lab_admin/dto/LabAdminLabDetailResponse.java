package com.labhub.CveLabhubBack.lab_admin.dto;

import java.time.Instant;

public record LabAdminLabDetailResponse(
        String labUuid,
        Integer cveId,
        String cveName,
        String instanceId,
        String region,
        String status,
        Instant createdAt,
        Instant expiresAt,
        Instant terminatedAt,
        Integer maxTtlMinutes,
        Long ttlRemainingSeconds,
        Long ttlRemainingMinutes,
        Long userId,
        String userEmail,
        String userDisplayName
) {
}


