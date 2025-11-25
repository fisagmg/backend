package com.labhub.CveLabhubBack.cve_lab.dto.lab_time;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시간 연장 가능 여부 응답")
public record LabExtendableResponse(
        @Schema(description = "연장 가능 여부", example = "true")
        boolean extendable,
        
        @Schema(description = "실습 잔여 시간(분)", example = "45")
        long remainingMinutes,
        
        @Schema(description = "최대 연장 가능 시간(분)", example = "60")
        int maxExtendMinutes
) {}

