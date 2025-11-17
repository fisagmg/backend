package com.labhub.CveLabhubBack.cve_lab.scheduler;

import com.labhub.CveLabhubBack.cve_lab.service.LabSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LabSessionScheduler {
    
    private final LabSessionService labSessionService;
    
    /**
     * 만료된 Lab 세션 자동 종료
     * 매 1분마다 실행
     */
    @Scheduled(cron = "0 */1 * * * *")
    public void terminateExpiredSessions() {
        log.info("Starting scheduled task: terminate expired lab sessions");
        
        try {
            labSessionService.terminateExpiredSessions();
        } catch (Exception e) {
            log.error("Error during scheduled termination of expired lab sessions", e);
        }
        
        log.info("Completed scheduled task: terminate expired lab sessions");
    }
}

