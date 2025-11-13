package com.labhub.CveLabhubBack.cve_lab.repository;

import com.labhub.CveLabhubBack.cve_lab.entity.Cve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CveRepository extends JpaRepository<Cve, Integer> {
    Optional<Cve> findByName(String name);
}

