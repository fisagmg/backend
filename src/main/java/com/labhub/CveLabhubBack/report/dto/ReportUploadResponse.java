package com.labhub.CveLabhubBack.report.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportUploadResponse {

    private Long reportId;
    
    private String fileUrl;
    
    private Integer version;
    
    private String message;
}

