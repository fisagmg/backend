package com.labhub.CveLabhubBack.news.scheduler;

import com.labhub.CveLabhubBack.news.service.NaverNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final NaverNewsService naverNewsService;

    /**
     * 3시간마다 뉴스 수집 (0분 0초에 실행)
     */
    @Scheduled(cron = "0 0 */3 * * *")
    public void scheduledNewsCrawling() {
        log.info("⏰ [NewsScheduler] 뉴스 크롤링 스케줄 실행");
        naverNewsService.crawlAndSaveNews();
    }
}

