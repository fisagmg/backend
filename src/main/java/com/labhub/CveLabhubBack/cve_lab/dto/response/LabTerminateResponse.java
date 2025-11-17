package com.labhub.CveLabhubBack.cve_lab.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "실습 종료 응답")
public record LabTerminateResponse(
        @Schema(description = "종료 성공 여부", example = "true")
        boolean terminated,
        
        @Schema(description = "종료된 시각", example = "2025-01-01T10:05:23")
        @JsonProperty("terminated_at")
        LocalDateTime terminatedAt
) {}

