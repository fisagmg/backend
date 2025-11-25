package com.labhub.CveLabhubBack.cve_lab.dto.lab_time;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "시간 연장 응답")
public record LabExtendResponse(
        @Schema(description = "연장 후 종료 예정 시간", example = "2025-01-01T11:00:00")
        @JsonProperty("expires_at")
        LocalDateTime expiresAt,
        
        @Schema(description = "연장된 시간(분)", example = "30")
        @JsonProperty("extended_minutes")
        int extendedMinutes
) {}

