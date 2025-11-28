package com.labhub.CveLabhubBack.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "보고서 생성 요청 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCreateRequest {
    
    @Schema(description = "CVE ID", example = "CVE-2024-1234", required = true)
    private String cveId;
    
    @Schema(description = "보고서 이름", example = "CVE-2024-1234 취약점 분석 보고서", required = true)
    private String name;
}

