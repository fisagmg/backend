package com.labhub.CveLabhubBack.cve_lab.scheduler;

import com.labhub.CveLabhubBack.cve_lab.service.LabSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lab 세션 TTL 자동 종료 스케줄러
 * 1분마다 만료된 세션을 자동으로 종료합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LabSessionScheduler {
    
    private final LabSessionService labSessionService;
    
    /**
     * TTL 만료된 실습 세션 자동 종료
     * 실행 주기: 1분 (60,000ms)
     * 
     * 처리 로직:
     * 1. expires_at < now() AND status = ACTIVE 조회
     * 2. RunnerClient.destroy() 호출
     * 3. 성공 시: status → TERMINATED, terminated_at 설정
     * 4. 실패 시: 로그만 남기고 다음 스케줄에서 재시도
     */
    @Scheduled(fixedRate = 60000)
    public void terminateExpiredSessions() {
        log.info("=== Starting TTL-based Lab session auto-termination scheduler ===");
        
        try {
            labSessionService.terminateExpiredSessions();
            log.info("=== Completed TTL-based Lab session auto-termination scheduler ===");
        } catch (Exception e) {
            log.error("=== Error in TTL-based Lab session auto-termination scheduler ===", e);
            // 스케줄러는 멈추지 않고 다음 실행을 기다림
        }
    }
}

