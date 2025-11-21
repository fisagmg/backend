package com.labhub.CveLabhubBack.analysis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알람 분석 결과 Entity
 * 테이블: alarm_analysis
 */
@Entity
@Table(name = "alarm_analysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alarm_id", nullable = false, unique = true, length = 100)
    private String alarmId;

    @Column(name = "alarm_name", nullable = false, length = 100)
    private String alarmName;

    @Column(name = "instance_id", nullable = false, length = 50)
    private String instanceId;

    @Column(name = "metric_name", nullable = false, length = 50)
    private String metricName;

    @Column(name = "namespace", nullable = false, length = 100)
    private String namespace;

    @Column(name = "state", nullable = false, length = 20)
    private String state;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "alarm_timestamp", nullable = false)
    private LocalDateTime alarmTimestamp;

    // AI 분석 결과
    @Column(name = "analysis_summary", length = 500)
    private String analysisSummary;

    @Column(name = "analysis_severity", length = 20)
    private String analysisSeverity;

    @Column(name = "analysis_root_cause", columnDefinition = "TEXT")
    private String analysisRootCause;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_evidence", columnDefinition = "JSON")
    private List<String> analysisEvidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_recommendations", columnDefinition = "JSON")
    private List<String> analysisRecommendations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}