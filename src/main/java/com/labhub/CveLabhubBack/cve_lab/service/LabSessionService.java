package com.labhub.CveLabhubBack.cve_lab.service;

import com.labhub.CveLabhubBack.cve_lab.config.LabConfig;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabExtendableResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabExtendResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabRemainingTimeResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.response.LabTerminateResponse;
import com.labhub.CveLabhubBack.cve_lab.entity.Lab;
import com.labhub.CveLabhubBack.cve_lab.entity.LabStatus;
import com.labhub.CveLabhubBack.cve_lab.exception.LabExtensionNotAllowedException;
import com.labhub.CveLabhubBack.cve_lab.exception.LabNotFoundException;
import com.labhub.CveLabhubBack.cve_lab.exception.LabTerminatedException;
import com.labhub.CveLabhubBack.cve_lab.repository.LabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class LabSessionService {
    
    private final LabRepository labRepository;
    private final AwsEc2Service awsEc2Service;
    private final LabConfig labConfig;
    
    /**
     * 실습 잔여시간 조회
     */
    @Transactional(readOnly = true)
    public LabRemainingTimeResponse getRemainingTime(String uuid) {
        Lab lab = findLabByUuid(uuid);
        validateActive(lab);
        
        return new LabRemainingTimeResponse(lab.getExpiresAt());
    }
    
    /**
     * 시간 연장 가능 여부 체크
     */
    @Transactional(readOnly = true)
    public LabExtendableResponse isExtendable(String uuid) {
        Lab lab = findLabByUuid(uuid);
        
        // TERMINATED 상태면 연장 불가
        if (lab.getStatus() == LabStatus.TERMINATED) {
            return new LabExtendableResponse(false, 0, labConfig.getExtendUnitMinutes());
        }
        
        // 잔여 시간 계산
        long remainingMinutes = calculateRemainingMinutes(lab);
        
        // 연장 가능 여부 판단
        LocalDateTime potentialExpiresAt = lab.getExpiresAt().plusMinutes(labConfig.getExtendUnitMinutes());
        LocalDateTime maxAllowedTime = lab.getCreatedAt().plusMinutes(getMaxTtlMinutes(lab));
        boolean extendable = !potentialExpiresAt.isAfter(maxAllowedTime);
        
        return new LabExtendableResponse(
                extendable, 
                remainingMinutes, 
                labConfig.getExtendUnitMinutes()
        );
    }
    
    /**
     * 시간 연장
     */
    @Transactional
    public LabExtendResponse extendLabSession(String uuid) {
        Lab lab = findLabByUuid(uuid);
        validateActive(lab);
        
        // 연장 가능 여부 검증
        LocalDateTime potentialExpiresAt = lab.getExpiresAt().plusMinutes(labConfig.getExtendUnitMinutes());
        LocalDateTime maxAllowedTime = lab.getCreatedAt().plusMinutes(getMaxTtlMinutes(lab));
        
        if (potentialExpiresAt.isAfter(maxAllowedTime)) {
            throw new LabExtensionNotAllowedException(
                    "Cannot extend lab session beyond maximum TTL. Max allowed time: " + maxAllowedTime);
        }
        
        // 시간 연장
        lab.setExpiresAt(potentialExpiresAt);
        labRepository.save(lab);
        
        log.info("Lab session extended: uuid={}, newExpiresAt={}, extendedBy={}min", 
                uuid, potentialExpiresAt, labConfig.getExtendUnitMinutes());
        
        return new LabExtendResponse(potentialExpiresAt, labConfig.getExtendUnitMinutes());
    }
    
    /**
     * VM 종료 (수동) - 보고서 작성은 가능
     */
    @Transactional
    public LabTerminateResponse terminateLabSession(String uuid) {
        Lab lab = findLabByUuid(uuid);
        
        // 이미 종료된 세션은 재종료 불가
        if (!lab.isTerminatable()) {
            throw new LabTerminatedException("Lab is already terminated: " + uuid);
        }
        
        // AWS EC2 종료
        String userId = String.valueOf(lab.getUser().getId());
        awsEc2Service.terminateInstance(uuid, lab.getCveName(), userId);
        
        // 종료 처리
        LocalDateTime terminatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        lab.terminate(terminatedAt);
        labRepository.save(lab);
        
        log.info("VM terminated: uuid={}, terminatedAt={}", uuid, terminatedAt);
        
        return new LabTerminateResponse(true, terminatedAt);
    }
    
    /**
     * 실습 완료 - VM 종료 + 마이페이지 기록
     */
    @Transactional
    public LabTerminateResponse completeLabSession(String uuid) {
        Lab lab = findLabByUuid(uuid);
        
        // ACTIVE 상태만 완료 가능
        if (!lab.isTerminatable()) {
            throw new LabTerminatedException("Lab is already terminated: " + uuid);
        }
        
        // AWS EC2 종료
        String userId = String.valueOf(lab.getUser().getId());
        try {
            awsEc2Service.terminateInstance(uuid, lab.getCveName(), userId);
        } catch (Exception e) {
            log.warn("Failed to terminate VM during completion: uuid={}", uuid, e);
        }
        
        // 종료 처리 - terminated_at에 기록
        LocalDateTime terminatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        lab.terminate(terminatedAt);
        labRepository.save(lab);
        
        log.info("Lab session completed: uuid={}, terminatedAt={}", uuid, terminatedAt);
        
        return new LabTerminateResponse(true, terminatedAt);
    }
    
    /**
     * TTL 만료된 세션 자동 종료 (스케줄러용)
     */
    @Transactional
    public void terminateExpiredSessions() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        
        // ACTIVE 상태이면서 만료된 세션 조회
        List<Lab> expiredLabs = labRepository.findAllByStatusAndExpiresAtBefore(LabStatus.ACTIVE, now);
        
        log.info("Found {} expired ACTIVE lab sessions to terminate", expiredLabs.size());
        
        for (Lab lab : expiredLabs) {
            try {
                // AWS EC2 종료
                String userId = String.valueOf(lab.getUser().getId());
                
                try {
                    awsEc2Service.terminateInstance(lab.getUuid(), lab.getCveName(), userId);
                    log.info("Successfully called destroy for expired lab: uuid={}", lab.getUuid());
                    
                    // 종료 처리
                    lab.terminate(now);
                    labRepository.save(lab);
                    
                    log.info("Expired lab terminated: uuid={}, expiresAt={}", 
                        lab.getUuid(), lab.getExpiresAt());
                    
                } catch (Exception destroyEx) {
                    log.error("Failed to destroy expired lab: uuid={}. Will retry next schedule.", 
                        lab.getUuid(), destroyEx);
                }
                
            } catch (Exception e) {
                log.error("Unexpected error while terminating expired lab: uuid={}", 
                    lab.getUuid(), e);
            }
        }
        
        log.info("Completed processing {} expired lab sessions", expiredLabs.size());
    }
    
    // === Helper Methods ===
    
    private Lab findLabByUuid(String uuid) {
        return labRepository.findByUuid(uuid)
                .orElseThrow(() -> new LabNotFoundException(uuid));
    }
    
    private void validateActive(Lab lab) {
        if (lab.getStatus() != LabStatus.ACTIVE) {
            throw new LabTerminatedException(
                "Lab session is not active: " + lab.getUuid() 
                + ", status: " + lab.getStatus());
        }
    }
    
    private long calculateRemainingMinutes(Lab lab) {
        if (lab.getExpiresAt() == null) {
            return 0;
        }
        // Asia/Seoul로 통일하여 비교
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        Duration duration = Duration.between(now, lab.getExpiresAt());
        return Math.max(0, duration.toMinutes());
    }
    
    private int getMaxTtlMinutes(Lab lab) {
        return lab.getMaxTtlMinutes() != null 
                ? lab.getMaxTtlMinutes() 
                : labConfig.getMaxTtlMinutes();
    }
}

