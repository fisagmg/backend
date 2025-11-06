package com.labhub.CveLabhubBack.report.repository;

import com.labhub.CveLabhubBack.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 사용자 ID로 활성 상태의 보고서 목록 조회
     */
    @Query("SELECT r FROM Report r WHERE r.userId = :userId AND r.status = 'active' ORDER BY r.updatedAt DESC")
    List<Report> findActiveReportsByUserId(@Param("userId") Long userId);

    /**
     * ID와 사용자 ID로 보고서 조회 (보안을 위해)
     */
    @Query("SELECT r FROM Report r WHERE r.id = :id AND r.userId = :userId AND r.status = 'active'")
    Optional<Report> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * CVE ID와 사용자 ID로 보고서 조회
     */
    @Query("SELECT r FROM Report r WHERE r.cveId = :cveId AND r.userId = :userId AND r.status = 'active'")
    List<Report> findByCveIdAndUserId(@Param("cveId") String cveId, @Param("userId") Long userId);
}

