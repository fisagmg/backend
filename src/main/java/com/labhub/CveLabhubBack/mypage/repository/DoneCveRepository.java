package com.labhub.CveLabhubBack.mypage.repository;

import com.labhub.CveLabhubBack.mypage.entity.DoneCve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoneCveRepository extends JpaRepository<DoneCve, DoneCve.DoneCveId> {
    
    /**
     * 사용자 ID로 완료된 CVE 목록 조회 (CVE 정보 포함, 완료일 기준 내림차순)
     */
    @Query("SELECT dc FROM DoneCve dc " +
           "JOIN FETCH dc.cve " +
           "WHERE dc.userId = :userId " +
           "ORDER BY dc.finishedAt DESC")
    List<DoneCve> findByUserIdWithCveOrderByFinishedAtDesc(@Param("userId") Long userId);
    
    /**
     * 사용자 ID로 완료된 CVE 목록 조회 (기본 메서드, N+1 문제 가능)
     */
    List<DoneCve> findByUserIdOrderByFinishedAtDesc(Long userId);
    
    /**
     * 사용자 ID와 CVE ID로 조회
     */
    Optional<DoneCve> findByUserIdAndCveId(Long userId, Integer cveId);
    
    /**
     * 사용자 ID로 완료한 CVE 개수 조회
     */
    long countByUserId(Long userId);
}

