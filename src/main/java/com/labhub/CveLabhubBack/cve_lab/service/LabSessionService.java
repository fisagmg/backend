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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
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
        validateActiveOrCreated(lab);
        
        return new LabRemainingTimeResponse(lab.getExpiresAt());
    }
    
    /**
     * 시간 연장 가능 여부 체크
     */
    @Transactional(readOnly = true)
    public LabExtendableResponse isExtendable(String uuid) {
        Lab lab = findLabByUuid(uuid);
        
        // 완료되거나 취소된 세션은 연장 불가
        if (lab.getStatus() == LabStatus.COMPLETED || lab.getStatus() == LabStatus.CANCELLED) {
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
        validateActiveOrCreated(lab);
        
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
     * VM 종료 (수동) - LabStatus는 ACTIVE 유지
     */
    @Transactional
    public LabTerminateResponse terminateLabSession(String uuid) {
        Lab lab = findLabByUuid(uuid);
        
        // 이미 완료/취소/종료된 세션은 재종료 불가
        if (lab.getStatus() == LabStatus.COMPLETED
                || lab.getStatus() == LabStatus.CANCELLED
                || lab.getStatus() == LabStatus.TERMINATED) {
            throw new LabTerminatedException("Lab session already completed or cancelled: " + uuid);
        }
        
        // AWS EC2 종료
        String userId = String.valueOf(lab.getUser().getId());
        awsEc2Service.terminateInstance(uuid, lab.getCveName(), userId);
        
        // DB 상태 업데이트 (LabStatus는 TERMINATED로 변경)
        LocalDateTime terminatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        lab.setStatus(LabStatus.TERMINATED);
        lab.setTerminatedAt(terminatedAt);
        labRepository.save(lab);
        
        log.info("VM terminated: uuid={}, terminatedAt={}, status=TERMINATED", uuid, terminatedAt);
        
        return new LabTerminateResponse(true, terminatedAt);
    }
    
    /**
     * 실습 완료 - ACTIVE → COMPLETED, VM 자동 종료
     */
    @Transactional
    public LabTerminateResponse completeLabSession(String uuid) {
        Lab lab = findLabByUuid(uuid);
        
        // ACTIVE 또는 TERMINATED 상태만 완료 가능
        if (lab.getStatus() != LabStatus.ACTIVE && lab.getStatus() != LabStatus.TERMINATED) {
            throw new LabTerminatedException("Only ACTIVE or TERMINATED lab sessions can be completed: " + uuid + ", current status: " + lab.getStatus());
        }
        
        // AWS EC2 종료
        String userId = String.valueOf(lab.getUser().getId());
        awsEc2Service.terminateInstance(uuid, lab.getCveName(), userId);
        
        // DB 상태 업데이트: ACTIVE → COMPLETED
        LocalDateTime completedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        lab.setStatus(LabStatus.COMPLETED);
        lab.setTerminatedAt(completedAt);
        labRepository.save(lab);
        
        log.info("Lab session completed: uuid={}, completedAt={}", uuid, completedAt);
        
        return new LabTerminateResponse(true, completedAt);
    }
    
    /**
     * 실습 취소 - ACTIVE/CREATED → CANCELLED, VM 바로 종료
     */
    @Transactional
    public LabTerminateResponse cancelLabSession(String uuid) {
        Lab lab = findLabByUuid(uuid);
        
        // ACTIVE, CREATED, TERMINATED 상태만 취소 가능
        if (lab.getStatus() != LabStatus.ACTIVE
                && lab.getStatus() != LabStatus.CREATED
                && lab.getStatus() != LabStatus.TERMINATED) {
            throw new LabTerminatedException("Only ACTIVE, CREATED, or TERMINATED lab sessions can be cancelled: " + uuid + ", current status: " + lab.getStatus());
        }
        
        // AWS EC2 종료
        String userId = String.valueOf(lab.getUser().getId());
        try {
            awsEc2Service.terminateInstance(uuid, lab.getCveName(), userId);
        } catch (Exception e) {
            log.warn("Failed to terminate VM during cancellation, but continuing with status update: uuid={}", uuid, e);
            // VM 종료 실패해도 상태는 CANCELLED로 변경
        }
        
        // DB 상태 업데이트: ACTIVE/CREATED → CANCELLED
        LocalDateTime cancelledAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        lab.setStatus(LabStatus.CANCELLED);
        lab.setTerminatedAt(cancelledAt);
        labRepository.save(lab);
        
        log.info("Lab session cancelled: uuid={}, cancelledAt={}", uuid, cancelledAt);
        
        return new LabTerminateResponse(true, cancelledAt);
    }
    
    /**
     * 만료된 세션 자동 종료 (스케줄러용)
     * ACTIVE 또는 CREATED 상태의 만료된 세션을 CANCELLED로 변경
     */
    @Transactional
    public void terminateExpiredSessions() {
        // Asia/Seoul로 통일
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        List<Lab> expiredLabs = labRepository.findAllByStatusAndExpiresAtBefore(LabStatus.ACTIVE, now);
        
        // CREATED 상태의 만료된 세션도 조회 (만료 시간이 지났다면)
        List<Lab> expiredCreatedLabs = labRepository.findAllByStatusAndExpiresAtBefore(LabStatus.CREATED, now);
        expiredLabs.addAll(expiredCreatedLabs);
        
        log.info("Found {} expired lab sessions to terminate", expiredLabs.size());
        
        for (Lab lab : expiredLabs) {
            try {
                // AWS EC2 종료
                String userId = String.valueOf(lab.getUser().getId());
                try {
                    awsEc2Service.terminateInstance(lab.getUuid(), lab.getCveName(), userId);
                } catch (Exception e) {
                    log.warn("Failed to terminate VM for expired lab session, but continuing with status update: uuid={}", lab.getUuid(), e);
                }
                
                // DB 상태 업데이트: 만료된 세션은 CANCELLED로 변경
                lab.setStatus(LabStatus.CANCELLED);
                lab.setTerminatedAt(now);
                labRepository.save(lab);
                
                log.info("Expired lab session cancelled: uuid={}, expiresAt={}", 
                        lab.getUuid(), lab.getExpiresAt());
                        
            } catch (Exception e) {
                log.error("Failed to cancel expired lab session: uuid={}", lab.getUuid(), e);
                // 다음 반복으로 계속 진행
            }
        }
    }
    
    // === Helper Methods ===
    
    private Lab findLabByUuid(String uuid) {
        return labRepository.findByUuid(uuid)
                .orElseThrow(() -> new LabNotFoundException(uuid));
    }
    
    private void validateActiveOrCreated(Lab lab) {
        if (lab.getStatus() != LabStatus.ACTIVE && lab.getStatus() != LabStatus.CREATED) {
            throw new LabTerminatedException("Lab session is not active or created: " + lab.getUuid() + ", status: " + lab.getStatus());
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

