package com.labhub.CveLabhubBack.cve_lab.repository;

import com.labhub.CveLabhubBack.cve_lab.entity.Lab;
import com.labhub.CveLabhubBack.cve_lab.entity.LabStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface LabRepository extends JpaRepository<Lab, String> {
    Optional<Lab> findByUuid(String uuid);

    // 만료된 ACTIVE 상태의 Lab 세션 조회
    List<Lab> findAllByStatusAndExpiresAtBefore(LabStatus status, LocalDateTime now);
}

