package com.labhub.CveLabhubBack.report.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "report",
        indexes = {
                @Index(name = "idx_report_user_id", columnList = "user_id"),
                @Index(name = "idx_report_cve_id", columnList = "cve_id"),
                @Index(name = "idx_report_status", columnList = "status"),
                @Index(name = "idx_report_user_cve", columnList = "user_id, cve_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cve_id", nullable = false)
    private String cveId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Soft delete 처리
     */
    public void softDelete() {
        this.status = "deleted";
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 버전 증가
     */
    public void incrementVersion() {
        this.version++;
    }
}

