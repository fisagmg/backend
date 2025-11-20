package com.labhub.CveLabhubBack.mypage.dto;

import com.labhub.CveLabhubBack.mypage.entity.DoneCve;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletedCveResponse {
    private Integer cveId;
    private String cveName;        // CVE 테이블의 name 필드
    private String outline;         // CVE 설명
    private Float cvssScore;        // CVSS 점수
    private String labOs;           // 실습 OS
    private String relatedDomain;    // 관련 도메인
    private LocalDateTime completedAt; // 완료일 (finished_at)

    public static CompletedCveResponse fromEntity(DoneCve doneCve) {
        if (doneCve.getCve() == null) {
            throw new IllegalStateException("CVE 정보가 로드되지 않았습니다. fetch join이 필요합니다.");
        }

        return CompletedCveResponse.builder()
                .cveId(doneCve.getCve().getId())
                .cveName(doneCve.getCve().getName())
                .outline(doneCve.getCve().getOutline())
                .cvssScore(doneCve.getCve().getCvssScore())
                .labOs(doneCve.getCve().getLabOs())
                .relatedDomain(doneCve.getCve().getRelatedDomain())
                .completedAt(doneCve.getFinishedAt())
                .build();
    }
}
