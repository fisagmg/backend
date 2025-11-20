package com.labhub.CveLabhubBack.cve_lab.service;

import com.labhub.CveLabhubBack.auth.Repository.UserRepository;
import com.labhub.CveLabhubBack.auth.entity.UserEntity;
import com.labhub.CveLabhubBack.cve.repository.CveRepository;
import com.labhub.CveLabhubBack.cve_lab.client.RunnerClient;
import com.labhub.CveLabhubBack.cve.entity.Cve;
import com.labhub.CveLabhubBack.cve_lab.dto.LabCreateResponse;
import com.labhub.CveLabhubBack.cve_lab.entity.Lab;
import com.labhub.CveLabhubBack.cve_lab.entity.LabStatus;
import com.labhub.CveLabhubBack.cve_lab.dto.LabCreateRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.RunRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.RunResponse;
import com.labhub.CveLabhubBack.cve_lab.repository.LabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
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

    public LabCreateResponse create(String authenticatedUserId, String preferredUsername, LabCreateRequest req) {
        // 0. 유저 정보 resolve
        UserEntity user = resolveUser(authenticatedUserId);
        String uuid = UUID.randomUUID().toString();

        // 1. Runner 호출 → VM 생성
        RunRequest runnerRequest = new RunRequest(uuid, req.cveId(), String.valueOf(user.getId()));
        RunResponse runnerResponse = runnerClient.create(runnerRequest);
        log.info("Terraform runner create response: status={}, uuid={}, cveId={}, userId={}",
                runnerResponse.status(), runnerResponse.uuid(), runnerResponse.cveId(), user.getId());

        // DB에 Lab 정보 저장 (Lab 객체 반환받음)
        Lab lab = persistCreatedLab(user, req.cveId(), runnerResponse);

        // 2. 초기 응답 생성 (guacamoleUrl = null)
        LabCreateResponse labResponse = toLabCreateResponse(runnerResponse);

        // 3. Guacamole connection 생성 → 사용자 생성/권한 부여 → tunnel identifier 획득
        try {
            log.info("Starting Guacamole connection creation for user: {}", preferredUsername);
            String connectionId = guacamoleService.createConnection(labResponse, preferredUsername);
            log.info("Guacamole connection created successfully: connectionId={}", connectionId);
            
            // connectionId를 DB에 저장
            lab.setGuacamoleConnectionId(connectionId);
            labRepository.save(lab);
            log.info("Guacamole connectionId saved to Lab: uuid={}, connectionId={}", uuid, connectionId);
            
            // 4. 사용자 토큰으로 tunnel identifier 획득 후 URL 생성
            log.info("Building iframe URL with tunnel identifier for connectionId: {}", connectionId);
            String iframeUrl = guacamoleService.buildIframeUrl(connectionId, preferredUsername);
            log.info("Iframe URL created successfully: {}", iframeUrl);
            labResponse = labResponse.withGuacamoleUrl(iframeUrl);
        } catch (Exception ex) {
            log.error("Failed to create Guacamole connection for uuid {}: {}", uuid, ex.getMessage(), ex);
            log.error("Exception details: ", ex);
            // Guacamole 실패해도 VM은 생성되었으므로 응답 반환 (guacamoleUrl = null)
        }

        // 5. 반환
        return labResponse;
    }


    public RunResponse destroy(String authenticatedUserId, RunRequest req) {
        UserEntity user = resolveUser(authenticatedUserId);
        RunResponse response = runnerClient.destroy(new RunRequest(req.uuid(), req.cveId(), String.valueOf(user.getId())));
        log.info("Terraform runner destroy response: status={}, uuid={}, cveId={}, userId={}",
                response.status(), response.uuid(), response.cveId(), user.getId());

        updateDestroyedLab(user, response);
        
        // Guacamole connection 삭제 (저장된 connectionId 사용)
        try {
            Lab lab = labRepository.findByUuid(req.uuid())
                    .orElse(null);
            
            if (lab != null && lab.getGuacamoleConnectionId() != null && !lab.getGuacamoleConnectionId().isBlank()) {
                guacamoleService.deleteGuacSession(lab.getGuacamoleConnectionId());
                log.info("Guacamole connection deleted: uuid={}, connectionId={}", req.uuid(), lab.getGuacamoleConnectionId());
            } else {
                log.warn("Lab or Guacamole connectionId not found for uuid: {}", req.uuid());
            }
        } catch (Exception ex) {
            log.warn("Failed to delete Guacamole session for uuid {}: {}", req.uuid(), ex.getMessage());
        }
        return response;
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
                response.cveId(),
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


    private UserEntity resolveUser(String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new IllegalArgumentException("Authenticated userId is required.");
        }
        return findDatabaseUser(authenticatedUserId);
    }


    private UserEntity findDatabaseUser(String identifier) {
        return userRepository.findByKcUserId(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .map(user -> user)
                .orElseThrow(() -> new IllegalArgumentException("Cannot resolve userId for identifier: " + identifier));
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Lab persistCreatedLab(UserEntity user, String requestedCveName, RunResponse response) {
        Cve cve = cveRepository.findByName(requestedCveName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown CVE: " + requestedCveName));

        Lab lab = new Lab();
        lab.setUuid(response.uuid());
        lab.setCve(cve);
        lab.setCveId(cve.getId());
        lab.setUser(user);
        lab.setCveName(firstNonBlank(outputValue(response, "cve_id"), response.cveId()));
        lab.setInstanceId(requiredOutputValue(response, "instance_id"));
        lab.setRegion(requiredOutputValue(response, "region"));
        lab.setCreatedAt(parseDateTime(requiredOutputValue(response, "created_at")));
        
        // expires_at 설정: Terraform에서 받은 값이 없거나 null이면 기본값(8시간)으로 설정
        String expiresAtStr = outputValue(response, "expires_at");
        LocalDateTime expiresAt = parseDateTime(expiresAtStr);
        
        if (expiresAt == null) {
            LocalDateTime createdAt = lab.getCreatedAt();
            if (createdAt != null) {
                expiresAt = createdAt.plusMinutes(480); // 8시간 = 480분
                log.info("expires_at is null, setting default: {} (createdAt + 480 minutes)", expiresAt);
            } else {
                // createdAt도 null이면 현재 시간(Asia/Seoul) 기준으로 설정
                expiresAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusMinutes(480);
                log.warn("Both expires_at and createdAt are null, using current Asia/Seoul time + 480 minutes: {}", expiresAt);
            }
        }
        lab.setExpiresAt(expiresAt);
        
        // VM 생성 완료 시 바로 ACTIVE로 설정
        lab.setStatus(LabStatus.ACTIVE);

        return labRepository.save(lab);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDestroyedLab(UserEntity user, RunResponse response) {
        labRepository.findById(response.uuid()).ifPresentOrElse(lab -> {
            if (!lab.getUser().getId().equals(user.getId())) {
                log.warn("User {} attempted to terminate lab {} owned by {}", user.getId(), lab.getUuid(), lab.getUser().getId());
            }
            // VM 종료 시 LabStatus는 변경하지 않음 (ACTIVE 유지 또는 기존 상태 유지)
            // terminatedAt만 업데이트
            LocalDateTime terminatedAt = parseDateTime(firstNonBlank(outputValue(response, "terminated_at"), null));
            lab.setTerminatedAt(terminatedAt != null ? terminatedAt : LocalDateTime.now(ZoneId.of("Asia/Seoul")));
            labRepository.save(lab);
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


    private LocalDateTime parseDateTime(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) {
            return null;
        }
        try {
            // OffsetDateTime으로 파싱 후 Asia/Seoul로 변환
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(isoDateTime);
            // Asia/Seoul로 변환하여 LocalDateTime으로 변환
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
}
