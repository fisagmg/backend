package com.labhub.CveLabhubBack.cve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전체 CVE 개수를 반환하기 위한 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountDto {

    private Long count;
}



