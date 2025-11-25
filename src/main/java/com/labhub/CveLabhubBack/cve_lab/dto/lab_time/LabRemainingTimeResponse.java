package com.labhub.CveLabhubBack.cve_lab.dto.lab_time;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "실습 잔여시간 조회 응답")
public record LabRemainingTimeResponse(
        @Schema(description = "실습 종료 예정 시간", example = "2025-01-01T10:30:00")
        @JsonProperty("expires_at")
        LocalDateTime expiresAt
) {}

