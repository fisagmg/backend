package com.labhub.CveLabhubBack.cve.service;

import com.labhub.CveLabhubBack.auth.Repository.UserRepository;
import com.labhub.CveLabhubBack.cve.dto.CategoryStatsDto;
import com.labhub.CveLabhubBack.cve.dto.CountDto;
import com.labhub.CveLabhubBack.cve.dto.CveResponseDto;
import com.labhub.CveLabhubBack.cve.dto.UserCveProgressDto;
import com.labhub.CveLabhubBack.cve.entity.Cve;
import com.labhub.CveLabhubBack.cve.repository.CveRepository;
import com.labhub.CveLabhubBack.cve.repository.CveSpecification;
import com.labhub.CveLabhubBack.cve.util.SeverityCalculator;
import com.labhub.CveLabhubBack.mypage.repository.DoneCveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CVE 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CveService {

    private final CveRepository cveRepository;
    private final DoneCveRepository doneCveRepository;
    private final UserRepository userRepository;

    /**
     * 필터링된 CVE 목록 조회
     * 모든 필터는 optional이며, 제공되지 않으면 전체 목록 반환
     * 결과는 CVSS Score 내림차순으로 정렬됨
     *
     * @param relatedDomain 관련 도메인 필터 (optional)
     * @param year 연도 필터 (optional)
     * @param labOs Lab OS 필터 (optional)
     * @return 필터링된 CVE 목록
     */
    @Transactional(readOnly = true)
    public List<CveResponseDto> getFilteredCves(String relatedDomain, Integer year, String labOs) {
        log.info("CVE 목록 조회 - domain: {}, year: {}, os: {}", relatedDomain, year, labOs);
        
        Specification<Cve> spec = CveSpecification.withFilters(relatedDomain, year, labOs);
        List<Cve> cves = cveRepository.findAll(spec);
        
        log.info("조회된 CVE 개수: {}", cves.size());
        
        return cves.stream()
                .map(CveResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 전체 CVE 개수 조회
     *
     * @return 전체 CVE 개수
     */
    @Transactional(readOnly = true)
    public CountDto getTotalCount() {
        long count = cveRepository.count();
        log.info("전체 CVE 개수: {}", count);
        
        return CountDto.builder()
                .count(count)
                .build();
    }

    /**
     * CVE 카테고리별 통계 조회
     * 모든 CVE를 조회하여 CVSS Score를 기반으로 Severity를 계산하고 카테고리별로 집계
     *
     * @return 카테고리별 통계 (total, critical, high, medium)
     */
    @Transactional(readOnly = true)
    public CategoryStatsDto getCategoryStats() {
        log.info("CVE 카테고리 통계 조회");
        
        List<Cve> allCves = cveRepository.findAll();
        
        long critical = allCves.stream()
                .filter(cve -> SeverityCalculator.toSeverity(cve.getCvssScore()).equals("Critical"))
                .count();
        
        long high = allCves.stream()
                .filter(cve -> SeverityCalculator.toSeverity(cve.getCvssScore()).equals("High"))
                .count();
        
        long medium = allCves.stream()
                .filter(cve -> SeverityCalculator.toSeverity(cve.getCvssScore()).equals("Medium"))
                .count();
        
        long total = allCves.size();
        
        log.info("카테고리 통계 - Total: {}, Critical: {}, High: {}, Medium: {}", 
                total, critical, high, medium);
        
        return CategoryStatsDto.builder()
                .total(total)
                .critical(critical)
                .high(high)
                .medium(medium)
                .build();
    }

    /**
     * Challenge 통계 조회 (카테고리 통계와 동일한 형식)
     * Learning Page UI 상단에 사용되는 통계
     *
     * @return Challenge 통계 (total, critical, high, medium)
     */
    @Transactional(readOnly = true)
    public CategoryStatsDto getChallengeStats() {
        log.info("Challenge 통계 조회");
        // Challenge 통계는 카테고리 통계와 동일한 로직 사용
        return getCategoryStats();
    }

    /**
     * 사용자의 CVE 진행 상황 조회
     * JWT가 없으면(비로그인) completedCount는 null 반환
     * 
     * @param jwt JWT 토큰 (Optional - 없으면 null)
     * @return 완료한 CVE 개수 / 전체 CVE 개수
     */
    @Transactional(readOnly = true)
    public UserCveProgressDto getUserCveProgress(Jwt jwt) {
        Long completedCount = null;
        
        // JWT가 있으면 사용자의 완료 개수 조회
        if (jwt != null) {
            try {
                String email = getEmailFromJwt(jwt);
                Long userId = userRepository.findByEmail(email)
                        .map(user -> user.getId())
                        .orElse(null);
                
                if (userId != null) {
                    completedCount = doneCveRepository.countByUserId(userId);
                    log.info("사용자 CVE 진행 상황 - userId: {}, email: {}, completed: {}", 
                            userId, email, completedCount);
                }
            } catch (Exception e) {
                log.warn("JWT에서 사용자 정보 추출 실패: {}", e.getMessage());
                // JWT가 유효하지 않아도 totalCount는 반환
            }
        } else {
            log.info("비로그인 사용자 CVE 진행 상황 조회");
        }
        
        // 전체 CVE 개수는 항상 반환
        long totalCount = cveRepository.count();
        
        return UserCveProgressDto.builder()
                .completedCount(completedCount)  // 로그인 안 됐으면 null
                .totalCount(totalCount)
                .build();
    }

    /**
     * JWT에서 이메일 추출 (null 허용)
     */
    private String getEmailFromJwt(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }
        
        return email;
    }
}

