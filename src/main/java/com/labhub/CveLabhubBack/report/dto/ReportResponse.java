package com.labhub.CveLabhubBack.report.dto;

import com.labhub.CveLabhubBack.report.entity.Report;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Schema(description = "보고서 응답 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    @Schema(description = "보고서 ID", example = "1")
    private Long id;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "CVE ID", example = "CVE-2024-1234")
    private String cveId;

    @Schema(description = "보고서 이름", example = "CVE-2024-1234 취약점 분석 보고서")
    private String name;

    @Schema(description = "S3 파일 URL", example = "s3://bucket/reports/report-123.docx")
    private String fileUrl;

    @Schema(description = "보고서 상태", example = "active")
    private String status;

    @Schema(description = "보고서 버전", example = "1")
    private Integer version;

    @Schema(description = "생성 일시", example = "2024-11-12T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시", example = "2024-11-12T10:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "다운로드용 Presigned URL", example = "https://s3.amazonaws.com/bucket/report.docx?signature=...")
    private String presignedDownloadUrl;

    /**
     * Entity를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static ReportResponse fromEntity(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .userId(report.getUserId())
                .cveId(report.getCveId())
                .name(report.getName())
                .fileUrl(report.getFileUrl())
                .status(report.getStatus())
                .version(report.getVersion())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    /**
     * Entity와 presigned URL을 함께 받아서 DTO로 변환
     */
    public static ReportResponse fromEntityWithPresignedUrl(Report report, String presignedUrl) {
        ReportResponse response = fromEntity(report);
        response.setPresignedDownloadUrl(presignedUrl);
        return response;
    }
}

