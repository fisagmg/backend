package com.labhub.CveLabhubBack.report.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignedUrlResponse {

    private String presignedUrl;
    
    private Long expiresInMinutes;
    
    private String message;
}

