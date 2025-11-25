package com.labhub.CveLabhubBack.analysis.repository;

import com.labhub.CveLabhubBack.analysis.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 알람 분석 결과 Repository
 */
@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    /**
     * alarm_id로 조회
     */
    Optional<Analysis> findByAlarmId(String alarmId);

    /**
     * alarm_name으로 조회 (최신순)
     */
    List<Analysis> findByAlarmNameOrderByCreatedAtDesc(String alarmName);

    /**
     * instance_id로 조회 (최신순)
     */
    List<Analysis> findByInstanceIdOrderByCreatedAtDesc(String instanceId);

    /**
     * 특정 기간 내 알람 조회
     */
    List<Analysis> findByAlarmTimestampBetweenOrderByAlarmTimestampDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Severity별 조회
     */
    List<Analysis> findByAnalysisSeverityOrderByCreatedAtDesc(String severity);

    /**
     * 최근 N개 조회
     */
    List<Analysis> findTop10ByOrderByCreatedAtDesc();

    /**
     * 특정 알람이 이미 존재하는지 확인
     */
    boolean existsByAlarmId(String alarmId);
}