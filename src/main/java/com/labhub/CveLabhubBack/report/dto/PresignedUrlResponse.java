package com.labhub.CveLabhubBack.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Presigned URL 응답 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignedUrlResponse {

    @Schema(description = "다운로드용 Presigned URL", example = "https://s3.amazonaws.com/bucket/report.docx?signature=...")
    private String presignedUrl;
    
    @Schema(description = "URL 만료 시간 (분)", example = "15")
    private Long expiresInMinutes;
    
    @Schema(description = "응답 메시지", example = "다운로드 URL이 생성되었습니다.")
    private String message;
}

