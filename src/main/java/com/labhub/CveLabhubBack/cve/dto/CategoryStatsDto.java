package com.labhub.CveLabhubBack.cve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CVE 카테고리별 통계 정보를 반환하기 위한 DTO
 * Critical, High, Medium 3개 카테고리의 개수를 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatsDto {

    private Long total;
    private Long critical;
    private Long high;
    private Long medium;
}



