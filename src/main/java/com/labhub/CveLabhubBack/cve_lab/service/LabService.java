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

    @Value("${ec2.ssh.username:ubuntu}")
    private String defaultSshUsername;

    @Value("${ec2.ssh.password:}")
    private String defaultSshPassword;

    @PostConstruct
    void normalizeDefaultPrivateKey() {
        if (defaultPrivateKey != null) {
            defaultPrivateKey = defaultPrivateKey.replace("\\n", "\n");
        }
    }

    public LabCreateResponse create(String authenticatedUserId, LabCreateRequest req) {
        // 0. 유저 정보 resolve
        UserEntity user = resolveUser(authenticatedUserId);
        String uuid = UUID.randomUUID().toString();

        // 1. Runner 호출 → VM 생성
        RunRequest runnerRequest = new RunRequest(uuid, req.cveId(), String.valueOf(user.getId()));
        RunResponse runnerResponse = runnerClient.create(runnerRequest);
        log.info("Terraform runner create response: status={}, uuid={}, cveId={}, userId={}",
                runnerResponse.status(), runnerResponse.uuid(), runnerResponse.cveId(), user.getId());

        // DB에 Lab 정보 저장
        persistCreatedLab(user, req.cveId(), runnerResponse);

        // 2. 초기 응답 생성 (guacamoleUrl = null)
        LabCreateResponse labResponse = toLabCreateResponse(runnerResponse);

        // Keycloak의 preferred_username이 email이므로 email 사용
        String guacUsername = (user.getEmail() != null && !user.getEmail().isBlank())
                ? user.getEmail()
                : user.getKcUserId();

        // 3. Guacamole 세션 생성 → URL 반환
        try {
            String iframeUrl = guacamoleService.createGuacSession(guacUsername, labResponse);
            // 4. 최종 응답 완성
            labResponse = labResponse.withGuacamoleUrl(iframeUrl);
        } catch (Exception ex) {
            log.error("Failed to create Guacamole session for uuid {}: {}", uuid, ex.getMessage(), ex);
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
        try {
            guacamoleService.deleteGuacSession(req.uuid());
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
    public void persistCreatedLab(UserEntity user, String requestedCveName, RunResponse response) {
        Cve cve = cveRepository.findByName(requestedCveName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown CVE: " + requestedCveName));

        Lab lab = new Lab();
        lab.setUuid(response.uuid());
        lab.setCve(cve);
        lab.setUser(user);
        lab.setCveName(firstNonBlank(outputValue(response, "cve_id"), response.cveId()));
        lab.setInstanceId(requiredOutputValue(response, "instance_id"));
        lab.setRegion(requiredOutputValue(response, "region"));
        lab.setCreatedAt(parseDateTime(requiredOutputValue(response, "created_at")));
        lab.setExpiresAt(parseDateTime(outputValue(response, "expires_at")));
        lab.setStatus(LabStatus.from(outputValue(response, "status")));

        labRepository.save(lab);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDestroyedLab(UserEntity user, RunResponse response) {
        labRepository.findById(response.uuid()).ifPresentOrElse(lab -> {
            if (!lab.getUser().getId().equals(user.getId())) {
                log.warn("User {} attempted to terminate lab {} owned by {}", user.getId(), lab.getUuid(), lab.getUser().getId());
            }
            lab.setStatus(LabStatus.from(firstNonBlank(outputValue(response, "status"), response.status())));
            LocalDateTime terminatedAt = parseDateTime(firstNonBlank(outputValue(response, "terminated_at"), null));
            lab.setTerminatedAt(terminatedAt != null ? terminatedAt : LocalDateTime.now());
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
            return OffsetDateTime.parse(isoDateTime).toLocalDateTime();
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
