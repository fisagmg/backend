package com.labhub.CveLabhubBack.news.repository;

import com.labhub.CveLabhubBack.news.entity.CveNewsMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CveNewsMappingRepository extends JpaRepository<CveNewsMapping, CveNewsMapping.CveNewsMappingId> {

    boolean existsByNewsIdAndCveId(Long newsId, Long cveId);
}

