package com.labhub.CveLabhubBack.news.dto;

import com.labhub.CveLabhubBack.news.entity.News;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsResponse {
    
    private Long id;
    private String publisher;
    private String title;
    private String firstLine;
    private String thumbnail;
    private String externalUrl;
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

