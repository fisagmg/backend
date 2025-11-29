package com.labhub.CveLabhubBack.lab_admin.service;

import com.labhub.CveLabhubBack.cve_lab.entity.Lab;
import com.labhub.CveLabhubBack.cve_lab.entity.LabStatus;
import com.labhub.CveLabhubBack.cve_lab.exception.LabNotFoundException;
import com.labhub.CveLabhubBack.lab_admin.dto.LabAdminLabDetailResponse;
import com.labhub.CveLabhubBack.lab_admin.dto.LabAdminLabPageResponse;
import com.labhub.CveLabhubBack.lab_admin.dto.LabAdminLabSummaryResponse;
import com.labhub.CveLabhubBack.cve_lab.repository.LabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabAdminService {

    private final LabRepository labRepository;

    public LabAdminLabPageResponse getLabs(String status, Pageable pageable) {
        Page<Lab> page = resolveStatus(status)
                .map(labStatus -> labRepository.findAllByStatus(labStatus, pageable))
                .orElseGet(() -> labRepository.findAll(pageable));

        List<LabAdminLabSummaryResponse> content = page.getContent().stream()
                .map(this::toSummaryResponse)
                .toList();

        return new LabAdminLabPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public LabAdminLabDetailResponse getLabDetail(String labUuid) {
        Lab lab = labRepository.findByUuid(labUuid)
                .orElseThrow(() -> new LabNotFoundException(labUuid));
        return toDetailResponse(lab);
    }

    private LabAdminLabSummaryResponse toSummaryResponse(Lab lab) {
        Duration remaining = calculateRemainingDuration(lab);
        String displayName = buildDisplayName(lab);

        return new LabAdminLabSummaryResponse(
                lab.getUuid(),
                lab.getCveId(),
                lab.getCveName(),
                lab.getUser().getEmail(),
                displayName,
                lab.getInstanceId(),
                lab.getStatus().name(),
                lab.getCreatedAt(),
                lab.getExpiresAt(),
                toSeconds(remaining),
                toMinutes(remaining),
                lab.getStatus() == LabStatus.ACTIVE
        );
    }

    private LabAdminLabDetailResponse toDetailResponse(Lab lab) {
        Duration remaining = calculateRemainingDuration(lab);
        String displayName = buildDisplayName(lab);

        return new LabAdminLabDetailResponse(
                lab.getUuid(),
                lab.getCveId(),
                lab.getCveName(),
                lab.getInstanceId(),
                lab.getRegion(),
                lab.getStatus().name(),
                lab.getCreatedAt(),
                lab.getExpiresAt(),
                lab.getTerminatedAt(),
                lab.getMaxTtlMinutes(),
                toSeconds(remaining),
                toMinutes(remaining),
                lab.getUser().getId(),
                lab.getUser().getEmail(),
                displayName
        );
    }

    private Duration calculateRemainingDuration(Lab lab) {
        if (lab.getStatus() == LabStatus.TERMINATED || lab.getExpiresAt() == null) {
            return Duration.ZERO;
        }
        Instant now = Instant.now();
        Duration duration = Duration.between(now, lab.getExpiresAt());
        if (duration.isNegative()) {
            return Duration.ZERO;
        }
        return duration;
    }

    private Long toSeconds(Duration duration) {
        return duration != null ? duration.getSeconds() : null;
    }

    private Long toMinutes(Duration duration) {
        return duration != null ? duration.toMinutes() : null;
    }

    private String buildDisplayName(Lab lab) {
        String firstName = lab.getUser().getFirstName();
        String lastName = lab.getUser().getLastName();
        if (isBlank(firstName) && isBlank(lastName)) {
            return lab.getUser().getEmail();
        }
        if (isBlank(firstName)) {
            return lastName;
        }
        if (isBlank(lastName)) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    private Optional<LabStatus> resolveStatus(String status) {
        if (isBlank(status)) {
            return Optional.empty();
        }
        try {
            String normalized = status.trim().toUpperCase();
            if ("RUNNING".equals(normalized)) {
                normalized = LabStatus.ACTIVE.name();
            }
            return Optional.of(LabStatus.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("지원하지 않는 Lab 상태 값입니다: " + status);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}


