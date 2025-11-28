package com.labhub.CveLabhubBack.cve.controller;

import com.labhub.CveLabhubBack.cve.dto.CategoryStatsDto;
import com.labhub.CveLabhubBack.cve.dto.CountDto;
import com.labhub.CveLabhubBack.cve.dto.CveResponseDto;
import com.labhub.CveLabhubBack.cve.dto.UserCveProgressDto;
import com.labhub.CveLabhubBack.cve.service.CveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CVE Learning Page API 컨트롤러
 * CVE 목록 조회 및 통계 정보를 제공하는 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cve")
@RequiredArgsConstructor
@Tag(name = "CVE API", description = "CVE 정보 조회 및 통계 API")
public class CveController {

    private final CveService cveService;
    private final JwtDecoder jwtDecoder;

    /**
     * CVE 목록 조회 (필터링 지원)
     * 
     * @param domain 관련 도메인 필터 (optional, 예: WEB, NETWORK)
     * @param year 연도 필터 (optional, 예: 2024)
     * @param os Lab OS 필터 (optional, 예: LINUX, WINDOWS)
     * @return 필터링된 CVE 목록 (CVSS Score 내림차순 정렬)
     */
    @Operation(
        summary = "CVE 목록 조회",
        description = "필터 조건에 따라 CVE 목록을 조회합니다. 모든 필터는 optional이며, 제공되지 않으면 전체 목록을 반환합니다. " +
                      "결과는 CVSS Score 내림차순으로 정렬됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "CVE 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CveResponseDto.class)
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<List<CveResponseDto>> getFilteredCves(
            @Parameter(description = "관련 도메인 (예: WEB, NETWORK)", example = "WEB")
            @RequestParam(required = false) String domain,
            
            @Parameter(description = "연도 (예: 2024)", example = "2024")
            @RequestParam(required = false) Integer year,
            
            @Parameter(description = "Lab OS (예: LINUX, WINDOWS)", example = "LINUX")
            @RequestParam(required = false) String os
    ) {
        log.info("GET /api/v1/cve - domain: {}, year: {}, os: {}", domain, year, os);
        List<CveResponseDto> cves = cveService.getFilteredCves(domain, year, os);
        return ResponseEntity.ok(cves);
    }

    /**
     * 전체 CVE 개수 조회
     * 
     * @return 전체 CVE 개수
     */
    @Operation(
        summary = "전체 CVE 개수 조회",
        description = "데이터베이스에 저장된 전체 CVE의 개수를 반환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "CVE 개수 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CountDto.class)
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/stats/count")
    public ResponseEntity<CountDto> getTotalCount() {
        log.info("GET /api/v1/cve/stats/count");
        CountDto count = cveService.getTotalCount();
        return ResponseEntity.ok(count);
    }

    /**
     * CVE 카테고리별 통계 조회
     * 
     * @return 카테고리별 통계 (total, critical, high, medium)
     */
    @Operation(
        summary = "CVE 카테고리별 통계 조회",
        description = "CVE를 Severity(Critical, High, Medium)별로 집계한 통계를 반환합니다. " +
                      "Severity는 CVSS Score를 기반으로 계산됩니다: " +
                      "Critical(9.0-10.0), High(7.0-8.9), Medium(4.0-6.9)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "카테고리 통계 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CategoryStatsDto.class)
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/stats/categories")
    public ResponseEntity<CategoryStatsDto> getCategoryStats() {
        log.info("GET /api/v1/cve/stats/categories");
        CategoryStatsDto stats = cveService.getCategoryStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Challenge 통계 조회
     * Learning Page UI 상단에 표시되는 통계 정보
     * 
     * @return Challenge 통계 (total, critical, high, medium)
     */
    @Operation(
        summary = "Challenge 통계 조회",
        description = "Learning Page UI 상단에 표시되는 Challenge 통계를 반환합니다. " +
                      "카테고리별 통계와 동일한 형식입니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Challenge 통계 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CategoryStatsDto.class)
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/stats/challenges")
    public ResponseEntity<CategoryStatsDto> getChallengeStats() {
        log.info("GET /api/v1/cve/stats/challenges");
        CategoryStatsDto stats = cveService.getChallengeStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 현재 로그인한 사용자의 CVE 진행 상황 조회
     * Learning Page 메인에 "1/29" 또는 "-" 형태로 표시
     * 로그인하지 않은 경우에도 호출 가능 (completedCount는 null 반환)
     * 
     * @param authorizationHeader Authorization 헤더 (Optional - "Bearer {token}")
     * @return 완료한 CVE 개수 / 전체 CVE 개수
     */
    @Operation(
        summary = "사용자 CVE 진행 상황 조회",
        description = "현재 로그인한 사용자가 완료한 CVE 개수와 전체 CVE 개수를 반환합니다. " +
                      "로그인하지 않은 경우 completedCount는 null이 반환됩니다 (프론트엔드에서 '-'로 표시). " +
                      "Learning Page 메인에 '1/29' 또는 '-' 형태로 표시됩니다. " +
                      "Authorization 헤더에 'Bearer {token}' 형식으로 JWT를 전달하면 사용자별 진행 상황을 반환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "CVE 진행 상황 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserCveProgressDto.class)
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/stats/progress")
    public ResponseEntity<UserCveProgressDto> getUserCveProgress(
            @Parameter(description = "JWT Bearer 토큰 (Optional)", example = "Bearer eyJhbGc...")
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        Jwt jwt = null;
        
        // Authorization 헤더가 있으면 JWT 파싱 시도
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String token = authorizationHeader.substring(7); // "Bearer " 제거
                jwt = jwtDecoder.decode(token);
                log.info("GET /api/v1/cve/stats/progress - user: {}", jwt.getClaimAsString("email"));
            } catch (Exception e) {
                log.warn("JWT 파싱 실패, 비로그인 사용자로 처리: {}", e.getMessage());
                // JWT가 유효하지 않아도 계속 진행 (비로그인 처리)
            }
        } else {
            log.info("GET /api/v1/cve/stats/progress - anonymous user");
        }
        
        UserCveProgressDto progress = cveService.getUserCveProgress(jwt);
        return ResponseEntity.ok(progress);
    }
}

