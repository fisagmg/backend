package com.labhub.CveLabhubBack.cve_lab.service;

import com.labhub.CveLabhubBack.auth.Repository.UserRepository;
import com.labhub.CveLabhubBack.auth.entity.UserEntity;
import com.labhub.CveLabhubBack.cve.repository.CveRepository;
import com.labhub.CveLabhubBack.cve_lab.client.RunnerClient;
import com.labhub.CveLabhubBack.cve.entity.Cve;
import com.labhub.CveLabhubBack.cve_lab.config.LabConfig;
import com.labhub.CveLabhubBack.cve_lab.dto.lab_run.LabCreateResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.lab_time.LabExtendableResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.lab_time.LabExtendResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.lab_time.LabRemainingTimeResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.lab_time.LabTerminateResponse;
import com.labhub.CveLabhubBack.cve_lab.entity.Lab;
import com.labhub.CveLabhubBack.cve_lab.entity.LabStatus;
import com.labhub.CveLabhubBack.cve_lab.dto.lab_run.LabCreateRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.lab_run.RunRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.lab_run.RunResponse;
import com.labhub.CveLabhubBack.cve_lab.exception.LabExtensionNotAllowedException;
import com.labhub.CveLabhubBack.cve_lab.exception.LabNotFoundException;
import com.labhub.CveLabhubBack.cve_lab.exception.LabTerminatedException;
import com.labhub.CveLabhubBack.cve_lab.repository.LabRepository;
import com.labhub.CveLabhubBack.mypage.entity.DoneCve;
import com.labhub.CveLabhubBack.mypage.repository.DoneCveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabService {

    private final RunnerClient runnerClient;
    private final UserRepository userRepository;
    private final LabRepository labRepository;
    private final CveRepository cveRepository;
    private final GuacamoleService guacamoleService;
    private final LabConfig labConfig;
    private final DoneCveRepository doneCveRepository;

    @Value("${ec2.ssh.private-key:}")
    private String defaultPrivateKey;

    @Value("${ec2.ssh.username:}")
    private String defaultSshUsername;

    @Value("${ec2.ssh.password:}")
    private String defaultSshPassword;

    @PostConstruct
    void normalizeDefaultPrivateKey() {
        if (defaultPrivateKey != null) {
            // 줄바꿈 문자 정규화
            defaultPrivateKey = defaultPrivateKey.replace("\\n", "\n");
            // 이스케이프된 인용부호 제거 (환경변수에서 문자열로 저장된 경우)
            defaultPrivateKey = defaultPrivateKey.replace("\\\"", "\"");
            // 앞뒤 불필요한 인용부호 제거
            defaultPrivateKey = defaultPrivateKey.trim();
            if (defaultPrivateKey.startsWith("\"") && defaultPrivateKey.endsWith("\"")) {
                defaultPrivateKey = defaultPrivateKey.substring(1, defaultPrivateKey.length() - 1);
            }
        }
    }

    public LabCreateResponse create(String kcUserId, String userEmail, LabCreateRequest req) {
        // 0. 유저 정보 db에서 가져오기
        UserEntity user = findDatabaseUser(kcUserId);
        String uuid = UUID.randomUUID().toString();

        // 1. Runner 호출 → VM 생성
        RunRequest runnerRequest = new RunRequest(uuid, req.cveName(), String.valueOf(user.getId()));
        log.info("------------------{}", req.cveName());
        RunResponse runnerResponse = runnerClient.create(runnerRequest);
        log.info("Terraform runner create response: status={}, uuid={}, cveName={}, userId={}, all={}",
                runnerResponse.status(), runnerResponse.uuid(), runnerResponse.cveName(), user.getId(), runnerResponse);

        // 2. DB에 Lab 정보 저장 (Lab 객체 반환받음)
        Lab lab = persistCreatedLab(user, req.cveName(), runnerResponse);

        // 2. 초기 응답 생성 (guacamoleUrl = null)
        LabCreateResponse labResponse = toLabCreateResponse(runnerResponse);

        // 3. Guacamole connection 생성 → 사용자 생성/권한 부여 → tunnel identifier 획득
        try {
            String connectionId = guacamoleService.createConnection(labResponse);
            log.info("Guacamole connection created successfully: connectionId={}", connectionId);
            
            // connectionId를 DB에 저장
            lab.setGuacamoleConnectionId(connectionId);
            labRepository.save(lab);
            log.info("Guacamole connectionId saved to Lab: uuid={}, connectionId={}", uuid, connectionId);
            
            // 4. 사용자 토큰으로 tunnel identifier 획득 후 URL 생성
            log.info("Building iframe URL with tunnel identifier for connectionId: {}", connectionId);
            String iframeUrl = guacamoleService.buildIframeUrl(connectionId, userEmail);
            log.info("Iframe URL created successfully: {}", iframeUrl);
            labResponse = labResponse.withGuacamoleUrl(iframeUrl);
        } catch (Exception ex) {
            log.error("Failed to create Guacamole connection for uuid {}: {}", uuid, ex.getMessage(), ex);
            log.error("Exception details: ", ex);
        }

        return labResponse;
    }

    /** VM 종료 */
    @Transactional
    public RunResponse destroy(String kcUserId, RunRequest req) {
        UserEntity user = findDatabaseUser(kcUserId);
        
        // 1. Lab 엔티티 조회 (Guacamole connectionId를 위해 먼저 조회)
        Lab lab = labRepository.findByUuid(req.uuid())
                .orElseThrow(() -> new IllegalArgumentException("Lab not found: " + req.uuid()));
        
        // 2. 이미 종료된 세션인지 확인
        if (lab.getStatus() == LabStatus.TERMINATED) {
            log.warn("Lab session already terminated: uuid={}", req.uuid());
            throw new IllegalStateException("Lab session already terminated: " + req.uuid());
        }
        
        // 3. VM 삭제 (Terraform Runner 호출)
        RunResponse response = runnerClient.destroy(new RunRequest(req.uuid(), req.cveName(), String.valueOf(user.getId())));
        log.info("Terraform runner destroy response: status={}, uuid={}, cveName={}, userId={}",
                response.status(), response.uuid(), response.cveName(), user.getId());

        // 4. DB 상태 업데이트 (status → TERMINATED, terminatedAt 설정)
        updateDestroyedLab(user, response);

        // 5. Guacamole connection 삭제
        guacamoleService.deleteConnectionForLab(lab);

        return response;
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
                // VM 삭제 (Terraform Runner 호출)
                RunRequest request = new RunRequest(uuid, lab.getCveName(), String.valueOf(userId));
                // destroy 컨트롤러 호출
                runnerClient.destroy(request);
                vmTerminated = true;
                log.info("VM terminated for completion: uuid={}", uuid);

                // Guacamole connection 삭제
                guacamoleService.deleteConnectionForLab(lab);
            } catch (Exception e) {
                log.warn("VM termination failed during completion: uuid={}, error={}", uuid, e.getMessage());
            }
        }

        if (vmTerminated) {
            lab.setStatus(LabStatus.TERMINATED);
            lab.setTerminatedAt(finishedAt);
            labRepository.save(lab);
        }

        // done_cve 추가
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


    /**
     * RunResponse를 LabCreateResponse로 변환
     *
     * 초기 생성 시 guacamoleUrl은 null로 설정
     * SSH 접속 정보는 환경변수에서만 가져옴 (Terraform 응답에는 없음)
     */
    private LabCreateResponse toLabCreateResponse(RunResponse response) {
        String privateIp = requiredOutputValue(response, "private_ip");
        String hostname = firstNonBlank(outputValue(response, "hostname"), privateIp);
        String instanceId = requiredOutputValue(response, "instance_id");
        String status = firstNonBlank(outputValue(response, "status"), response.status());

        // SSH 접속 정보는 환경변수에서만 가져옴 (Terraform runner 응답에는 없음)
        String sshUsername = (defaultSshUsername != null && !defaultSshUsername.isBlank())
                ? defaultSshUsername
                : "ubuntu";

        // 현재는 사용하지 않음
        String sshPassword = (defaultSshPassword != null && !defaultSshPassword.isBlank())
                ? defaultSshPassword
                : null;

        // privateKey는 항상 환경변수에서 가져옴
        String privateKey = (defaultPrivateKey != null && !defaultPrivateKey.isBlank())
                ? defaultPrivateKey
                : null;

        return new LabCreateResponse(
                response.uuid(),
                response.cveName(),
                privateIp,
                hostname,
                instanceId,
                status,
                response.tfstatePath(),
                null,  // guacamoleUrl 초기값: null (나중에 GuacamoleService에서 설정)
                sshUsername,
                sshPassword,
                privateKey
        );
    }

    // keycloak id로 db에서 user entity 찾기
    private UserEntity findDatabaseUser(String kcUserId) {
        return userRepository.findByKcUserId(kcUserId)
                .orElseThrow(() -> new IllegalArgumentException("Cannot resolve userId for kcUserId: " + kcUserId));
    }

    // DB에 lab 정보 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Lab persistCreatedLab(UserEntity user, String cveName, RunResponse response) {
        Cve cve = cveRepository.findByName(cveName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown CVE: " + cveName));

        Lab lab = new Lab();
        lab.setUuid(response.uuid());
        lab.setCve(cve);
        lab.setCveId(cve.getId());
        lab.setUser(user);
        lab.setCveName(firstNonBlank(outputValue(response, "cve_id"), response.cveName()));
        lab.setInstanceId(requiredOutputValue(response, "instance_id"));
        lab.setRegion(requiredOutputValue(response, "region"));
        lab.setCreatedAt(parseDateTime(requiredOutputValue(response, "created_at")));

        // expires_at 설정 - 항상 application.properties의 설정값 사용
        // Terraform 응답의 expires_at은 무시하고 일관된 시간 정책 적용
        LocalDateTime createdAt = lab.getCreatedAt();
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
            lab.setCreatedAt(createdAt);
        }

        // application.properties의 lab.initial-ttl-minutes 설정값을 항상 사용
        LocalDateTime expiresAt = createdAt.plusMinutes(labConfig.getInitialTtlMinutes());
        log.info("Setting expires_at using initial TTL from config: {} minutes (createdAt: {}, expiresAt: {})",
                labConfig.getInitialTtlMinutes(), createdAt, expiresAt);

        lab.setExpiresAt(expiresAt);

        // VM 생성 완료 시 바로 ACTIVE로 설정
        lab.setStatus(LabStatus.ACTIVE);

        return labRepository.save(lab);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDestroyedLab(UserEntity user, RunResponse response) {
        labRepository.findByUuid(response.uuid()).ifPresentOrElse(lab -> {
            if (!lab.getUser().getId().equals(user.getId())) {
                log.warn("User {} attempted to terminate lab {} owned by {}", user.getId(), lab.getUuid(), lab.getUser().getId());
            }
            // VM 종료 시 status를 TERMINATED로 변경하고 terminatedAt 설정
            LocalDateTime terminatedAt = parseDateTime(firstNonBlank(outputValue(response, "terminated_at"), null));
            if (terminatedAt == null) {
                terminatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
            }
            lab.terminate(terminatedAt);
            labRepository.save(lab);
            log.info("Lab status updated to TERMINATED: uuid={}, terminatedAt={}", lab.getUuid(), terminatedAt);
        }, () -> log.warn("Lab with uuid {} not found during destroy handling.", response.uuid()));
    }


    private String outputValue(RunResponse response, String key) {
        if (response.outputs() == null) {
            return null;
        }
        RunResponse.RunnerOutput output = response.outputs().get(key);
        return output != null ? output.value() : null;
    }


    private String requiredOutputValue(RunResponse response, String key) {
        String value = outputValue(response, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Runner response missing required output: " + key);
        }
        return value;
    }


    // DateTime region Asia/Seoul로 변환
    private LocalDateTime parseDateTime(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) {
            return null;
        }
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(isoDateTime);
            return offsetDateTime.atZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            log.warn("Failed to parse datetime '{}': {}", isoDateTime, ex.getMessage());
            return null;
        }
    }


    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

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

                // VM 삭제 (Terraform Runner 호출)
                try {
                    RunRequest request = new RunRequest(lab.getUuid(), lab.getCveName(), userId);
                    runnerClient.destroy(request);
                    log.info("VM terminated for expired session: uuid={}", lab.getUuid());
                } catch (Exception e) {
                    log.warn("Failed to terminate VM for expired session: uuid={}, err={}",
                            lab.getUuid(), e.getMessage());
                }

                // Guacamole connection 삭제
                guacamoleService.deleteConnectionForLab(lab);

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

    // ===== Helper Methods for Session Management =====

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

