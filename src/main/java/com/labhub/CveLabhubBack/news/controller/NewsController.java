package com.labhub.CveLabhubBack.news.controller;

import com.labhub.CveLabhubBack.news.dto.NewsResponse;
import com.labhub.CveLabhubBack.news.service.NaverNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "News", description = "CVE 관련 뉴스 조회 및 크롤링 API")
@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NaverNewsService naverNewsService;

    @Operation(summary = "최신 4개 뉴스 조회", description = "가장 최근에 크롤링된 CVE 관련 뉴스 4개를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/top")
    public ResponseEntity<List<NewsResponse>> getTop4News() {
        log.info("GET /api/news/top - 최신 4개 뉴스 조회");
        return ResponseEntity.ok(naverNewsService.getTop4News());
    }

    @Operation(summary = "전체 뉴스 조회", description = "DB에 저장된 모든 CVE 관련 뉴스를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<List<NewsResponse>> getAllNews() {
        log.info("GET /api/news - 전체 뉴스 조회");
        return ResponseEntity.ok(naverNewsService.getAllNews());
    }

    @Operation(summary = "수동 뉴스 크롤링", description = "네이버 뉴스에서 CVE 관련 뉴스를 즉시 크롤링하여 DB에 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "크롤링 성공"),
            @ApiResponse(responseCode = "500", description = "크롤링 실패")
    })
    @PostMapping("/crawl")
    public ResponseEntity<String> triggerManualCrawl() {
        log.info("POST /api/news/crawl - 수동 크롤링 실행");
        naverNewsService.crawlAndSaveNews();
        return ResponseEntity.ok("뉴스 크롤링이 완료되었습니다.");
    }
}

