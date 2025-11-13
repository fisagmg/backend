package com.labhub.CveLabhubBack.news.repository;

import com.labhub.CveLabhubBack.news.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    boolean existsByExternalUrl(String externalUrl);
    
    List<News> findTop4ByOrderByCreatedAtDesc();
    
    List<News> findAllByOrderByCreatedAtDesc();
}

