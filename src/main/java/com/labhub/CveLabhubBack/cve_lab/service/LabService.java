package com.labhub.CveLabhubBack.cve_lab.service;

import com.labhub.CveLabhubBack.auth.Repository.UserRepository;
import com.labhub.CveLabhubBack.auth.entity.UserEntity;
import com.labhub.CveLabhubBack.cve.repository.CveRepository;
import com.labhub.CveLabhubBack.cve_lab.client.RunnerClient;
import com.labhub.CveLabhubBack.cve.entity.Cve;
import com.labhub.CveLabhubBack.cve_lab.entity.Lab;
import com.labhub.CveLabhubBack.cve_lab.entity.LabStatus;
import com.labhub.CveLabhubBack.cve_lab.dto.LabCreateRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.RunRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.RunResponse;
import com.labhub.CveLabhubBack.cve_lab.repository.LabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    public RunResponse create(String authenticatedUserId, LabCreateRequest req) {
        UserEntity user = resolveUser(authenticatedUserId);
        String userId = String.valueOf(user.getId());
        String uuid = UUID.randomUUID().toString();

        RunRequest runnerRequest = new RunRequest(uuid, req.cveId(), userId);
        RunResponse response = runnerClient.create(runnerRequest);

        log.info("Terraform runner create response: status={}, uuid={}, cveId={}, userId={}",
                response.status(), response.uuid(), response.cveId(), userId);

        persistCreatedLab(user, req.cveId(), response);

        return response;
    }

    public RunResponse destroy(String authenticatedUserId, RunRequest req) {
        UserEntity user = resolveUser(authenticatedUserId);
        String userId = String.valueOf(user.getId());
        RunRequest runnerRequest = new RunRequest(req.uuid(), req.cveId(), userId);
        RunResponse response = runnerClient.destroy(runnerRequest);

        log.info("Terraform runner destroy response: status={}, uuid={}, cveId={}, userId={}",
                response.status(), response.uuid(), response.cveId(), userId);

        updateDestroyedLab(user, response);

        return response;
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
