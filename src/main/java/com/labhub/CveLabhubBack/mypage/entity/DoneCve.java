package com.labhub.CveLabhubBack.mypage.entity;

import com.labhub.CveLabhubBack.auth.entity.UserEntity;
import com.labhub.CveLabhubBack.cve.entity.Cve;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "done_cve")
@IdClass(DoneCve.DoneCveId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DoneCve {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "cve_id", nullable = false)
    private Integer cveId;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cve_id", insertable = false, updatable = false)
    private Cve cve;

    /**
     * 복합 Primary Key를 위한 내부 클래스
     * done_cve 테이블은 (user_id, cve_id) 두 개의 컬럼으로 PK를 구성
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class DoneCveId implements Serializable {
        private Long userId;
        private Integer cveId;
    }
}

