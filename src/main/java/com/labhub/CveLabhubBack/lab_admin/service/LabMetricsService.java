package com.labhub.CveLabhubBack.lab_admin.service;

import com.labhub.CveLabhubBack.cve_lab.entity.Lab;
import com.labhub.CveLabhubBack.cve_lab.exception.LabNotFoundException;
import com.labhub.CveLabhubBack.lab_admin.config.CloudWatchClientProvider;
import com.labhub.CveLabhubBack.lab_admin.config.CloudWatchLogsClientProvider;
import com.labhub.CveLabhubBack.lab_admin.dto.LabMetricPoint;
import com.labhub.CveLabhubBack.lab_admin.dto.LabMetricsResponse;
import com.labhub.CveLabhubBack.lab_admin.dto.LabLogEvent;
import com.labhub.CveLabhubBack.lab_admin.dto.LabLogStreamResponse;
import com.labhub.CveLabhubBack.cve_lab.repository.LabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.DimensionFilter;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataResult;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CloudWatchLogsException;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabMetricsService {

    private static final int DEFAULT_PERIOD_SECONDS = 60;
    private static final Duration DEFAULT_RANGE = Duration.ofHours(1);
    private static final List<String> LOG_STREAM_SUFFIXES = List.of("syslog", "auth", "docker");
    private static final int LOG_EVENT_LIMIT = 200;

    private enum MetricKey {
        CPU("cpu", "CPU_USAGE"),
        MEMORY("memory", "MEMORY_USED"),
        DISK("disk", "DISK_USED");

        private final String responseKey;
        private final String metricName;

        MetricKey(String responseKey, String metricName) {
            this.responseKey = responseKey;
            this.metricName = metricName;
        }
    }

    private final LabRepository labRepository;
    private final CloudWatchClientProvider cloudWatchClientProvider;
    private final CloudWatchLogsClientProvider cloudWatchLogsClientProvider;

    @Value("${aws.cloudwatch.logs.log-group:cvexpert}")
    private String logGroupPrefix;

    @Value("${aws.cloudwatch.metrics.namespace.prefix:cvexpert}")
    private String namespacePrefix;

    @Transactional(readOnly = true)
    public LabMetricsResponse getLabMetrics(String labUuid, Duration range) {
        Lab lab = labRepository.findByUuid(labUuid)
                .orElseThrow(() -> new LabNotFoundException(labUuid));

        CloudWatchClient client = cloudWatchClientProvider.getClient(lab.getRegion());

        // CloudWatch는 UTC 시간을 사용하지만, 서울 지역의 시간대를 고려하여 조회
        // Lab 생성 시간(UTC)을 서울 시간대로 변환하여 로깅 및 디버깅에 사용
        ZoneId seoulZone = ZoneId.of("Asia/Seoul");
        
        // 현재 UTC 시간
        Instant end = Instant.now();
        
        // Lab 생성 시간(UTC) - 이미 UTC이므로 그대로 사용
        Instant labStartTime = lab.getCreatedAt();
        
        // 최대 조회 범위: 현재로부터 3일 전
        Instant maxStartTime = end.minus(Duration.ofDays(3));
        
        // 시작 시간 결정: Lab 생성 시간과 3일 전 중 더 늦은 시간
        Instant start = labStartTime.isAfter(maxStartTime) ? labStartTime : maxStartTime;
        
        // 실제 조회된 시간 범위 계산 (분 단위)
        long actualRangeMinutes = Duration.between(start, end).toMinutes();

        // 디버깅을 위한 시간 정보 로깅 (UTC와 서울 시간 모두 표시)
        ZonedDateTime startSeoul = start.atZone(seoulZone);
        ZonedDateTime endSeoul = end.atZone(seoulZone);
        ZonedDateTime labCreatedSeoul = labStartTime.atZone(seoulZone);
        
        log.info("메트릭 조회 범위 - Lab: {}", labUuid);
        log.info("  - Lab생성 시간: UTC={}, 서울={}", labStartTime, labCreatedSeoul);
        log.info("  - 조회 시작 시간: UTC={}, 서울={}", start, startSeoul);
        log.info("  - 조회 종료 시간: UTC={}, 서울={}", end, endSeoul);
        log.info("  - 조회 범위: {}분 ({}시간)", actualRangeMinutes, actualRangeMinutes / 60.0);

        // CVE ID 가져오기 (예: "CVE-2025-1302")
        String cveId = lab.getCveName();
        String instanceId = lab.getInstanceId();
        
        // CVE별 동적 namespace 생성: cvexpert/CVE-2025-1302 (EC2 제거)
        String metricNamespace = buildNamespaceForLab(cveId);
        log.info("메트릭 namespace: {} (Lab: {}, CVE: {})", metricNamespace, labUuid, cveId);

        // Get actual disk dimensions from CloudWatch
        Map<String, String> actualDiskDimensions = getActualDiskDimensions(client, lab.getInstanceId(), metricNamespace);

        List<MetricDataQuery> queries = buildQueries(lab.getInstanceId(), actualDiskDimensions, metricNamespace);
        
        // CloudWatch 요청 상세 로그
        log.info("CloudWatch 메트릭 요청 정보:");
        log.info("  - Region: {} (서울: ap-northeast-2)", lab.getRegion());
        log.info("  - InstanceId: {}", lab.getInstanceId());
        log.info("  - Namespace: {}", metricNamespace);
        log.info("  - StartTime: UTC={} (서울={})", start, startSeoul);
        log.info("  - EndTime: UTC={} (서울={})", end, endSeoul);
        log.info("  - TimeRange: {} minutes ({} hours)", actualRangeMinutes, String.format("%.2f", actualRangeMinutes / 60.0));
        log.info("  - Query 개수: {}", queries.size());
        for (MetricDataQuery query : queries) {
            log.info("  - Query[{}]: namespace={}, metricName={}, dimensions={}, period={}s, stat={}",
                    query.id(),
                    query.metricStat().metric().namespace(),
                    query.metricStat().metric().metricName(),
                    query.metricStat().metric().dimensions(),
                    query.metricStat().period(),
                    query.metricStat().stat());
        }
        
        GetMetricDataResponse response;
        try {
            response = client.getMetricData(GetMetricDataRequest.builder()
                    .startTime(start)
                    .endTime(end)
                    .metricDataQueries(queries)
                    .build());
            
            log.info("CloudWatch 응답 - 성공: metricDataResults 개수={}", response.metricDataResults().size());
        } catch (CloudWatchException ex) {
            String errorMessage = ex.awsErrorDetails() != null
                    ? ex.awsErrorDetails().errorMessage()
                    : ex.getMessage();
            log.error("CloudWatch 메트릭 조회 실패 - labUuid={}, instanceId={}, region={}, reason={}",
                    labUuid, lab.getInstanceId(), lab.getRegion(), errorMessage, ex);
            throw new IllegalStateException("CloudWatch 메트릭 조회 중 오류가 발생했습니다.", ex);
        }

        Map<MetricKey, List<LabMetricPoint>> series = extractSeries(response);
        List<LabLogStreamResponse> logs = fetchLogs(lab, instanceId, start, end);

        String finalDiskPath = actualDiskDimensions.getOrDefault("path", "/");
        String finalDiskDevice = actualDiskDimensions.getOrDefault("device", "");
        String finalDiskFstype = actualDiskDimensions.getOrDefault("fstype", "");

        log.info("메트릭 조회 결과 - CPU: {}개, Memory: {}개, Disk: {}개, Logs: {}개 스트림", 
                series.getOrDefault(MetricKey.CPU, List.of()).size(),
                series.getOrDefault(MetricKey.MEMORY, List.of()).size(),
                series.getOrDefault(MetricKey.DISK, List.of()).size(),
                logs.size());
        log.info("디스크 정보 - path: {}, device: {}, fstype: {} (from CloudWatch: {})", 
                finalDiskPath, finalDiskDevice, finalDiskFstype, actualDiskDimensions);

        return new LabMetricsResponse(
                lab.getUuid(),
                lab.getInstanceId(),
                lab.getRegion(),
                actualRangeMinutes,
                series.getOrDefault(MetricKey.CPU, List.of()),
                series.getOrDefault(MetricKey.MEMORY, List.of()),
                series.getOrDefault(MetricKey.DISK, List.of()),
                finalDiskPath,
                finalDiskDevice,
                finalDiskFstype,
                logs
        );
    }

    /**
     * CVE ID를 기반으로 CloudWatch 메트릭 namespace 생성
     * 형식: cvexpert/CVE-2025-1302 (EC2 제거)
     * 예: "CVE-2025-1302" → "cvexpert/CVE-2025-1302"
     */
    private String buildNamespaceForLab(String cveId) {
        if (cveId == null || cveId.isBlank()) {
            // CVE ID가 없는 경우 기본 prefix만 반환
            return namespacePrefix;
        }
        // namespacePrefix + "/" + CVE ID (대소문자 그대로 유지)
        return namespacePrefix + "/" + cveId;
    }

    /**
     * Agent 설정에 맞춘 CloudWatch 로그 그룹 이름 생성
     * Agent 설정: /aws/ec2/cve-lab/{instanceId}/{suffix}
     * 형식: /aws/ec2/cve-lab/i-06f582c3.../syslog
     */
    private String buildLogGroupName(String instanceId, String suffix) {
        return "/aws/ec2/cve-lab/" + instanceId + "/" + suffix;
    }

    private Duration normalizeRange(Duration range) {
        if (range == null || range.isNegative() || range.isZero()) {
            return DEFAULT_RANGE;
        }
        if (range.compareTo(Duration.ofDays(1)) > 0) {
            return Duration.ofDays(1);
        }
        return range;
    }

    private Map<String, String> getActualDiskDimensions(CloudWatchClient client, String instanceId, String metricNamespace) {
        Map<String, String> dimensions = new HashMap<>();
        try {
            var listMetricsResponse = client.listMetrics(builder -> builder
                    .namespace(metricNamespace)
                    .metricName("DISK_USED")
                    .dimensions(DimensionFilter.builder()
                            .name("InstanceId")
                            .value(instanceId)
                            .build())
            );

            if (!listMetricsResponse.metrics().isEmpty()) {
                var metric = listMetricsResponse.metrics().get(0);
                for (var dim : metric.dimensions()) {
                    String name = dim.name();
                    String value = dim.value();
                    if ("path".equalsIgnoreCase(name)) {
                        dimensions.put("path", value);
                    } else if ("device".equalsIgnoreCase(name)) {
                        dimensions.put("device", value);
                    } else if ("fstype".equalsIgnoreCase(name)) {
                        dimensions.put("fstype", value);
                    }
                }
                log.debug("실제 디스크 dimension 조회 성공: {}", dimensions);
            } else {
                log.warn("CloudWatch에서 DISK_USED 메트릭을 찾을 수 없습니다. instanceId={}", instanceId);
            }
        } catch (CloudWatchException ex) {
            log.warn("디스크 dimension 조회 실패, 기본값 사용: {}", ex.getMessage());
        }
        return dimensions;
    }

    private List<MetricDataQuery> buildQueries(String instanceId, Map<String, String> diskDimensions, String metricNamespace) {
        List<MetricDataQuery> queries = new ArrayList<>();
        for (MetricKey key : MetricKey.values()) {
            MetricDataQuery query = buildMetricDataQuery(key, instanceId, diskDimensions, metricNamespace);
            queries.add(query);
            log.debug("메트릭 쿼리 생성 - 타입: {}, 메트릭: {}, Namespace: {}, Dimension 개수: {}", 
                    key.responseKey, key.metricName, metricNamespace, query.metricStat().metric().dimensions().size());
        }
        return queries;
    }

    private MetricDataQuery buildMetricDataQuery(MetricKey key, String instanceId, Map<String, String> diskDimensions, String metricNamespace) {
        List<Dimension> dimensions = new ArrayList<>();
        dimensions.add(Dimension.builder()
                .name("InstanceId")
                .value(instanceId)
                .build());

        // CPU_IDLE 메트릭은 추가 dimension이 필요하지 않을 수 있음
        // 실제 CloudWatch 메트릭 구조에 맞게 조정 필요
        if (key == MetricKey.CPU) {
            // CPU_IDLE 메트릭은 일반적으로 추가 dimension이 없거나 다른 형식일 수 있음
            // CloudWatch에서 실제 확인 필요
        }
        if (key == MetricKey.DISK) {
            // Use actual dimensions from CloudWatch
            String path = diskDimensions.get("path");
            String fstype = diskDimensions.get("fstype");
            String device = diskDimensions.get("device");

            if (!isBlank(path)) {
                dimensions.add(Dimension.builder()
                        .name("path")
                        .value(path)
                        .build());
            }
            if (!isBlank(fstype)) {
                dimensions.add(Dimension.builder()
                        .name("fstype")
                        .value(fstype)
                        .build());
            }
            if (!isBlank(device)) {
                dimensions.add(Dimension.builder()
                        .name("device")
                        .value(device)
                        .build());
            }
        }

        return MetricDataQuery.builder()
                .id(key.responseKey)
                .metricStat(MetricStat.builder()
                        .metric(Metric.builder()
                                .namespace(metricNamespace)
                                .metricName(key.metricName)
                                .dimensions(dimensions)
                                .build())
                        .period(DEFAULT_PERIOD_SECONDS)
                        .stat("Average")
                        .build())
                .returnData(true)
                .build();
    }

    private Map<MetricKey, List<LabMetricPoint>> extractSeries(GetMetricDataResponse response) {
        Map<MetricKey, List<LabMetricPoint>> series = new EnumMap<>(MetricKey.class);

        log.info("CloudWatch 메트릭 응답 처리 - 결과 개수: {}", response.metricDataResults().size());

        for (MetricDataResult result : response.metricDataResults()) {
            MetricKey key = resolveMetricKey(result.id());
            if (key == null) {
                log.warn("지원되지 않는 MetricDataResult id: {} (statusCode: {}, messages: {})", 
                        result.id(), result.statusCode(), result.messages());
                continue;
            }
            
            // CloudWatch 응답 상태 확인
            if (result.statusCode() != null && !result.statusCode().equals("Complete")) {
                log.warn("{} 메트릭 응답 상태: {} - 메시지: {}", 
                        key.responseKey, result.statusCode(), result.messages());
            }
            
            List<LabMetricPoint> points = new ArrayList<>();
            List<Instant> timestamps = result.timestamps();
            List<Double> values = result.values();
            
            log.info("{} 메트릭 - 데이터 포인트 개수: {}, statusCode: {}", 
                    key.responseKey, timestamps.size(), result.statusCode());
            
            if (timestamps.isEmpty() && values.isEmpty()) {
                log.warn("{} 메트릭에 데이터가 없습니다. (Label: {}, Messages: {})", 
                        key.responseKey, result.label(), result.messages());
            }
            
            for (int i = 0; i < Math.min(timestamps.size(), values.size()); i++) {
                points.add(new LabMetricPoint(timestamps.get(i), values.get(i)));
            }
            points.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));
            series.put(key, points);
        }

        return series;
    }

    private MetricKey resolveMetricKey(String id) {
        for (MetricKey key : MetricKey.values()) {
            if (key.responseKey.equalsIgnoreCase(id)) {
                return key;
            }
        }
        return null;
    }

    private List<LabLogStreamResponse> fetchLogs(Lab lab, String instanceId, Instant start, Instant end) {
        CloudWatchLogsClient client = cloudWatchLogsClientProvider.getClient(lab.getRegion());
        List<LabLogStreamResponse> streams = new ArrayList<>();

        for (String suffix : LOG_STREAM_SUFFIXES) {
            // Agent 설정에 맞춘 로그 그룹: /aws/ec2/cve-lab/{instanceId}/{suffix}
            String logGroup = buildLogGroupName(instanceId, suffix);
            // Agent 설정에 맞춘 로그 스트림: {instanceId} (suffix 없음)
            String logStream = instanceId;
            
            try {
                GetLogEventsResponse response = client.getLogEvents(GetLogEventsRequest.builder()
                        .logGroupName(logGroup)
                        .logStreamName(logStream)
                        .startTime(start.toEpochMilli())
                        .endTime(end.toEpochMilli())
                        .limit(LOG_EVENT_LIMIT)
                        .startFromHead(false)
                        .build());

                List<LabLogEvent> events = response.events().stream()
                        .map(this::toLogEvent)
                        .collect(Collectors.toList());

                streams.add(new LabLogStreamResponse(
                        logGroup,
                        logStream,
                        events
                ));
                
                log.info("로그 조회 성공 - logGroup: {}, logStream: {}, events: {}개", logGroup, logStream, events.size());
            } catch (CloudWatchLogsException ex) {
                String reason = ex.awsErrorDetails() != null
                        ? ex.awsErrorDetails().errorMessage()
                        : ex.getMessage();
                log.warn("CloudWatch 로그 조회 실패 - labUuid={}, logGroup={}, logStream={}, reason={}",
                        lab.getUuid(), logGroup, logStream, reason);
                streams.add(new LabLogStreamResponse(
                        logGroup,
                        logStream,
                        List.of()
                ));
            }
        }

        return streams;
    }

    private LabLogEvent toLogEvent(OutputLogEvent event) {
        Instant timestamp = event.timestamp() != null
                ? Instant.ofEpochMilli(event.timestamp())
                : null;
        Instant ingestion = event.ingestionTime() != null
                ? Instant.ofEpochMilli(event.ingestionTime())
                : null;
        return new LabLogEvent(timestamp, ingestion, event.message());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}


