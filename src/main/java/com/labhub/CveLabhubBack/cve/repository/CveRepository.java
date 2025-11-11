package com.labhub.CveLabhubBack.cve.repository;

import com.labhub.CveLabhubBack.cve.entity.Cve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CveRepository extends JpaRepository<Cve, Long> {

    Optional<Cve> findByYearAndNum(Integer year, Integer num);
}

