package com.labhub.CveLabhubBack.lab_admin.controller;

import com.labhub.CveLabhubBack.lab_admin.dto.LabAdminLabDetailResponse;
import com.labhub.CveLabhubBack.lab_admin.dto.LabAdminLabPageResponse;
import com.labhub.CveLabhubBack.lab_admin.dto.LabMetricsResponse;
import com.labhub.CveLabhubBack.lab_admin.service.LabAdminService;
import com.labhub.CveLabhubBack.lab_admin.service.LabMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/admin/labs")
@RequiredArgsConstructor
@Tag(name = "Lab Admin API", description = "관리자용 Lab 모니터링 API")
public class LabAdminController {

    private static final int MAX_PAGE_SIZE = 100;

    private final LabAdminService labAdminService;
    private final LabMetricsService labMetricsService;

    @GetMapping
    @Operation(summary = "Lab 목록 조회 (관리자)")
    public ResponseEntity<LabAdminLabPageResponse> getLabs(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.Direction.DESC, "createdAt"
        );
        return ResponseEntity.ok(labAdminService.getLabs(status, pageable));
    }

    @GetMapping("/{labUuid}")
    @Operation(summary = "Lab 상세 조회 (관리자)")
    public ResponseEntity<LabAdminLabDetailResponse> getLabDetail(
            @PathVariable("labUuid") String labUuid
    ) {
        return ResponseEntity.ok(labAdminService.getLabDetail(labUuid));
    }

    @GetMapping("/{labUuid}/metrics")
    @Operation(summary = "Lab CloudWatch 메트릭 조회 (관리자)")
    public ResponseEntity<LabMetricsResponse> getLabMetrics(
            @PathVariable("labUuid") String labUuid,
            @RequestParam(value = "range", defaultValue = "1h") String range
    ) {
        Duration duration = parseRange(range);
        return ResponseEntity.ok(labMetricsService.getLabMetrics(labUuid, duration));
    }

    private Duration parseRange(String rawRange) {
        if (rawRange == null || rawRange.isBlank()) {
            return Duration.ofHours(1);
        }

        String value = rawRange.trim().toLowerCase();
        
        // "all"인 경우 null 반환 (서비스에서 Lab 생성 시간부터 조회)
        if ("all".equals(value)) {
            return null;
        }
        
        try {
            if (value.endsWith("h")) {
                long hours = Long.parseLong(value.substring(0, value.length() - 1));
                return Duration.ofHours(hours);
            }
            if (value.endsWith("m")) {
                long minutes = Long.parseLong(value.substring(0, value.length() - 1));
                return Duration.ofMinutes(minutes);
            }
            if (value.endsWith("s")) {
                long seconds = Long.parseLong(value.substring(0, value.length() - 1));
                return Duration.ofSeconds(seconds);
            }
            long minutes = Long.parseLong(value);
            return Duration.ofMinutes(minutes);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("range 파라미터 형식이 올바르지 않습니다. 예: 1h, 30m, 3600s, all");
        }
    }
}


