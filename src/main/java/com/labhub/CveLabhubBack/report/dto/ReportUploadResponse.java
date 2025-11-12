package com.labhub.CveLabhubBack.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "보고서 업로드 응답 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportUploadResponse {

    @Schema(description = "보고서 ID", example = "1")
    private Long reportId;
    
    @Schema(description = "업로드된 S3 파일 URL", example = "s3://bucket/reports/report-123.docx")
    private String fileUrl;
    
    @Schema(description = "업데이트된 보고서 버전", example = "2")
    private Integer version;
    
    @Schema(description = "응답 메시지", example = "보고서가 성공적으로 업로드되었습니다.")
    private String message;
}

