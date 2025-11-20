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
import com.labhub.CveLabhubBack.cve.repository.CveRepository;
import com.labhub.CveLabhubBack.mypage.entity.DoneCve;
import com.labhub.CveLabhubBack.mypage.repository.DoneCveRepository;

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
    private final CveRepository cveRepository;
    private final LabConfig labConfig;
    private final DoneCveRepository doneCveRepository;

    /** 실습 잔여시간 조회 */
    @Transactional(readOnly = true)
    public LabRemainingTimeResponse getRemainingTime(String uuid) {
        Lab lab = findLabByUuid(uuid);
        validateActive(lab);
        return new LabRemainingTimeResponse(lab.getExpiresAt());
    }

    /** 시간 연장 가능 여부 체크 */
    @Transactional(readOnly = true)
    public LabExtendableResponse isExtendable(String uuid) {
        Lab lab = findLabByUuid(uuid);

        if (lab.getStatus() == LabStatus.TERMINATED) {
            return new LabExtendableResponse(false, 0, labConfig.getExtendUnitMinutes());
        }

        long remainingMinutes = calculateRemainingMinutes(lab);

        LocalDateTime potentialExpiresAt = lab.getExpiresAt().plusMinutes(labConfig.getExtendUnitMinutes());
        LocalDateTime maxAllowedTime = lab.getCreatedAt().plusMinutes(getMaxTtlMinutes(lab));

        boolean extendable = !potentialExpiresAt.isAfter(maxAllowedTime);

        return new LabExtendableResponse(
                extendable,
                remainingMinutes,
                labConfig.getExtendUnitMinutes()
        );
    }

    /** 시간 연장 */
    @Transactional
    public LabExtendResponse extendLabSession(String uuid) {
        Lab lab = findLabByUuid(uuid);
        validateActive(lab);

        LocalDateTime potentialExpiresAt = lab.getExpiresAt().plusMinutes(labConfig.getExtendUnitMinutes());
        LocalDateTime maxAllowedTime = lab.getCreatedAt().plusMinutes(getMaxTtlMinutes(lab));

        if (potentialExpiresAt.isAfter(maxAllowedTime)) {
            throw new LabExtensionNotAllowedException(
                    "Cannot extend lab session beyond maximum TTL. Max allowed time: " + maxAllowedTime);
        }

        lab.setExpiresAt(potentialExpiresAt);
        labRepository.save(lab);

        log.info("Lab session extended: uuid={}, newExpiresAt={}, extendedBy={}min",
                uuid, potentialExpiresAt, labConfig.getExtendUnitMinutes());

        return new LabExtendResponse(potentialExpiresAt, labConfig.getExtendUnitMinutes());
    }

    /** VM 수동 종료 */
    @Transactional
    public LabTerminateResponse terminateLabSession(String uuid) {
        Lab lab = findLabByUuid(uuid);

        if (lab.getStatus() == LabStatus.TERMINATED) {
            throw new LabTerminatedException("Lab session already terminated: " + uuid);
        }

        String userId = String.valueOf(lab.getUser().getId());
        awsEc2Service.terminateInstance(uuid, lab.getCveName(), userId);

        LocalDateTime terminatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        lab.terminate(terminatedAt);
        labRepository.save(lab);

        log.info("VM terminated: uuid={}, terminatedAt={}", uuid, terminatedAt);

        return new LabTerminateResponse(true, terminatedAt);
    }

    /** 실습 완료 */
    @Transactional
    public LabTerminateResponse completeLabSession(String uuid) {
        Lab lab = findLabByUuid(uuid);

        if (lab.getStatus() != LabStatus.ACTIVE && lab.getStatus() != LabStatus.TERMINATED) {
            throw new LabTerminatedException(
                    "Only ACTIVE or TERMINATED lab sessions can be completed: " + uuid
            );
        }

        Long userId = lab.getUser().getId();
        LocalDateTime finishedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        boolean vmTerminated = false;
        if (lab.getStatus() == LabStatus.ACTIVE) {
            try {
                awsEc2Service.terminateInstance(uuid, lab.getCveName(), String.valueOf(userId));
                vmTerminated = true;
            } catch (Exception e) {
                log.warn("VM termination failed during completion: uuid={}, error={}", uuid, e.getMessage());
            }
        }

        if (vmTerminated) {
            lab.setStatus(LabStatus.TERMINATED);
            lab.setTerminatedAt(finishedAt);
            labRepository.save(lab);
        }

        Integer cveId = cveRepository.findByName(lab.getCveName())
                .orElseThrow(() -> new IllegalStateException("CVE not found: " + lab.getCveName()))
                .getId();

        DoneCve doneCve = doneCveRepository.findByUserIdAndCveId(userId, cveId)
                .orElse(new DoneCve());

        doneCve.setUserId(userId);
        doneCve.setCveId(cveId);
        doneCve.setFinishedAt(finishedAt);
        doneCveRepository.save(doneCve);

        return new LabTerminateResponse(true, finishedAt);
    }

    /** 만료된 세션 자동 종료 */
    @Transactional
    public void terminateExpiredSessions() {

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        List<Lab> expiredLabs =
                labRepository.findAllByStatusAndExpiresAtBefore(LabStatus.ACTIVE, now);

        log.info("Found {} expired lab sessions to terminate", expiredLabs.size());

        for (Lab lab : expiredLabs) {
            try {
                String userId = String.valueOf(lab.getUser().getId());

                try {
                    awsEc2Service.terminateInstance(lab.getUuid(), lab.getCveName(), userId);
                } catch (Exception e) {
                    log.warn("Failed to terminate VM for expired session: uuid={}, err={}",
                            lab.getUuid(), e.getMessage());
                }

                lab.terminate(now);
                lab.setTerminatedAt(now);
                labRepository.save(lab);

                log.info("Expired lab session terminated: uuid={}", lab.getUuid());

            } catch (Exception e) {
                log.error("Failed to terminate expired lab session: uuid={}, err={}",
                        lab.getUuid(), e.getMessage());
            }
        }

        log.info("Completed processing {} expired lab sessions", expiredLabs.size());
    }

    // ===== Helpers =====

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
        if (lab.getExpiresAt() == null) return 0;

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
