package com.labhub.CveLabhubBack.cve.dto;

import com.labhub.CveLabhubBack.cve.entity.Cve;
import com.labhub.CveLabhubBack.cve.util.SeverityCalculator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CVE 정보를 클라이언트에게 반환하기 위한 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CveResponseDto {

    private String name;
    private Float cvssScore;
    private String severity;
    private String relatedDomain;
    private Integer year;
    private String labOs;
    private String outline;

    /**
     * Cve 엔티티로부터 CveResponseDto 생성
     * Severity는 CVSS Score를 기반으로 자동 계산됨
     *
     * @param cve CVE 엔티티
     * @return CveResponseDto
     */
    public static CveResponseDto from(Cve cve) {
        return CveResponseDto.builder()
                .name(cve.getName())
                .cvssScore(cve.getCvssScore())
                .severity(SeverityCalculator.toSeverity(cve.getCvssScore()))
                .relatedDomain(cve.getRelatedDomain())
                .year(cve.getYear())
                .labOs(cve.getLabOs())
                .outline(cve.getOutline())
                .build();
    }
}


