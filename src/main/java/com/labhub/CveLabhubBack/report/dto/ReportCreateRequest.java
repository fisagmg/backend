package com.labhub.CveLabhubBack.report.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCreateRequest {

    private Long userId;
    
    private String cveId;
    
    private String name;
}

