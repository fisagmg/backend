package com.labhub.CveLabhubBack.cve_lab.entity;

import com.labhub.CveLabhubBack.auth.entity.UserEntity;
import com.labhub.CveLabhubBack.cve.entity.Cve;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "lab")
public class Lab {

    @Id
    private String uuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LabStatus status = LabStatus.ACTIVE;

    @Column(name = "guacamole_connection_id", length = 255)
    private String guacamoleConnectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cve_id", nullable = false, insertable = false, updatable = false)
    private Cve cve;

    @Column(name = "cve_id", nullable = false, insertable = true, updatable = true)
    private Integer cveId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "cve_name", nullable = false, length = 20)
    private String cveName;

    @Column(name = "instance_id", nullable = false, length = 50)
    private String instanceId;

    @Column(nullable = false, length = 20)
    private String region;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Column(name = "max_ttl_minutes")
    private Integer maxTtlMinutes = 120;

    /**
     * 종료 가능 여부 확인 (TERMINATED 상태는 종료 불가)
     */
    public boolean isTerminatable() {
        return status == LabStatus.ACTIVE;
    }

    /**
     * VM 종료 처리
     */
    public void terminate(Instant terminatedAt) {
        if (!isTerminatable()) {
            throw new IllegalStateException("Lab is already terminated: " + uuid);
        }
        this.status = LabStatus.TERMINATED;
        this.terminatedAt = terminatedAt;
    }
}
