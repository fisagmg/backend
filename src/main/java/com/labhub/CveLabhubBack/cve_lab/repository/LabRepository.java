package com.labhub.CveLabhubBack.cve_lab.repository;

import com.labhub.CveLabhubBack.cve_lab.entity.Lab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabRepository extends JpaRepository<Lab, String> {
    Optional<Lab> findByUuid(String uuid);
}
