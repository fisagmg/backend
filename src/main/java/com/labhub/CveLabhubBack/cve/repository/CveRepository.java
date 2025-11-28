package com.labhub.CveLabhubBack.cve.repository;

import com.labhub.CveLabhubBack.cve.entity.Cve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CveRepository extends JpaRepository<Cve, Long>, JpaSpecificationExecutor<Cve> {

    Optional<Cve> findByYearAndNum(Integer year, Integer num);
    Optional<Cve> findByName(String name);
}

