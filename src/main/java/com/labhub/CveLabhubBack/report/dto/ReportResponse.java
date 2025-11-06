package com.labhub.CveLabhubBack.report.dto;

import com.labhub.CveLabhubBack.report.entity.Report;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private Long id;
    
    private Long userId;
    
    private String cveId;
    
    private String name;
    
    private String fileUrl;
    
    private String status;
    
    private Integer version;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
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

