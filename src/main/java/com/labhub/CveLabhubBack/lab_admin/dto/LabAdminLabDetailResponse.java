package com.labhub.CveLabhubBack.lab_admin.dto;

import java.time.LocalDateTime;

public record LabAdminLabDetailResponse(
        String labUuid,
        Integer cveId,
        String cveName,
        String instanceId,
        String region,
        String status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime terminatedAt,
        Integer maxTtlMinutes,
        Long ttlRemainingSeconds,
        Long ttlRemainingMinutes,
        Long userId,
        String userEmail,
        String userDisplayName
) {
}


