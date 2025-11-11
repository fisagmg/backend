package com.labhub.CveLabhubBack.news.controller;

import com.labhub.CveLabhubBack.news.dto.NewsResponse;
import com.labhub.CveLabhubBack.news.service.NaverNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NaverNewsService naverNewsService;

    @GetMapping("/top")
    public ResponseEntity<List<NewsResponse>> getTop4News() {
        log.info("GET /api/news/top - 최신 4개 뉴스 조회");
        return ResponseEntity.ok(naverNewsService.getTop4News());
    }

    @GetMapping
    public ResponseEntity<List<NewsResponse>> getAllNews() {
        log.info("GET /api/news - 전체 뉴스 조회");
        return ResponseEntity.ok(naverNewsService.getAllNews());
    }

    @PostMapping("/crawl")
    public ResponseEntity<String> triggerManualCrawl() {
        log.info("POST /api/news/crawl - 수동 크롤링 실행");
        naverNewsService.crawlAndSaveNews();
        return ResponseEntity.ok("뉴스 크롤링이 완료되었습니다.");
    }
}

