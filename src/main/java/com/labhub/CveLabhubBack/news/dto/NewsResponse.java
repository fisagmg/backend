package com.labhub.CveLabhubBack.news.dto;

import com.labhub.CveLabhubBack.news.entity.News;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Schema(description = "뉴스 응답 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsResponse {
    
    @Schema(description = "뉴스 ID", example = "1")
    private Long id;
    
    @Schema(description = "언론사", example = "보안뉴스")
    private String publisher;
    
    @Schema(description = "뉴스 제목", example = "CVE-2024-1234 취약점 발견")
    private String title;
    
    @Schema(description = "뉴스 첫 줄 내용", example = "최근 발견된 CVE-2024-1234 취약점은...")
    private String firstLine;
    
    @Schema(description = "썸네일 이미지 URL", example = "https://example.com/thumbnail.jpg")
    private String thumbnail;
    
    @Schema(description = "뉴스 원문 URL", example = "https://www.boannews.com/media/view.asp?idx=123456")
    private String externalUrl;
    
    @Schema(description = "뉴스 생성 일시", example = "2024-11-12T10:30:00")
    private LocalDateTime createdAt;
    
    public static NewsResponse fromEntity(News news) {
        return NewsResponse.builder()
                .id(news.getId())
                .publisher(news.getPublisher())
                .title(news.getTitle())
                .firstLine(news.getFirstLine())
                .thumbnail(news.getThumbnail())
                .externalUrl(news.getExternalUrl())
                .createdAt(news.getCreatedAt())
                .build();
    }
}

