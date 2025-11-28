package com.labhub.CveLabhubBack.cve.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 CVE 진행 상황 DTO
 * "1/29" 또는 "-" 형태로 표시될 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 값은 JSON에 포함하지 않음
public class UserCveProgressDto {

    /**
     * 사용자가 완료한 CVE 개수
     * 로그인하지 않은 경우 null (프론트엔드에서 "-"로 표시)
     */
    private Long completedCount;
    
    /**
     * 전체 CVE 개수
     * 항상 반환됨
     */
    private Long totalCount;
}



