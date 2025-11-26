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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabMetricsService {

    private static final String METRIC_NAMESPACE = "CVELabHub/EC2";
    private static final int DEFAULT_PERIOD_SECONDS = 60;
    private static final Duration DEFAULT_RANGE = Duration.ofHours(1);
    private static final List<String> LOG_STREAM_SUFFIXES = List.of("syslog", "auth", "cloud-init");
    private static final int LOG_EVENT_LIMIT = 200;

    private enum MetricKey {
        CPU("cpu", "cpu_usage_active"),
        MEMORY("memory", "mem_used_percent"),
        DISK("disk", "disk_used_percent");

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

    @Value("${aws.cloudwatch.logs.log-group:CVELabHub/EC2/Logs}")
    private String logGroupName;

    @Transactional(readOnly = true)
    public LabMetricsResponse getLabMetrics(String labUuid, Duration range) {
        Lab lab = labRepository.findByUuid(labUuid)
                .orElseThrow(() -> new LabNotFoundException(labUuid));

        CloudWatchClient client = cloudWatchClientProvider.getClient(lab.getRegion());

        Instant end = Instant.now();
        // Lab 생성 시간부터 현재까지의 모든 메트릭 조회 (최대 3일로 제한)
        Instant labStartTime = lab.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toInstant();
        Instant maxStartTime = end.minus(Duration.ofDays(3));
        Instant start = labStartTime.isAfter(maxStartTime) ? labStartTime : maxStartTime;
        
        // 실제 조회된 시간 범위 계산 (분 단위)
        long actualRangeMinutes = Duration.between(start, end).toMinutes();

        log.info("메트릭 조회 범위 - Lab: {}, Lab생성: {}, 조회시작: {}, 조회종료: {}, 범위: {}분", 
                labUuid, lab.getCreatedAt(), start, end, actualRangeMinutes);

        // Get actual disk dimensions from CloudWatch
        Map<String, String> actualDiskDimensions = getActualDiskDimensions(client, lab.getInstanceId());

        List<MetricDataQuery> queries = buildQueries(lab.getInstanceId(), actualDiskDimensions);
        GetMetricDataResponse response;
        try {
            response = client.getMetricData(GetMetricDataRequest.builder()
                    .startTime(start)
                    .endTime(end)
                    .metricDataQueries(queries)
                    .build());
        } catch (CloudWatchException ex) {
            String errorMessage = ex.awsErrorDetails() != null
                    ? ex.awsErrorDetails().errorMessage()
                    : ex.getMessage();
            log.error("CloudWatch 메트릭 조회 실패 - labUuid={}, instanceId={}, region={}, reason={}",
                    labUuid, lab.getInstanceId(), lab.getRegion(), errorMessage, ex);
            throw new IllegalStateException("CloudWatch 메트릭 조회 중 오류가 발생했습니다.", ex);
        }

        Map<MetricKey, List<LabMetricPoint>> series = extractSeries(response);
        List<LabLogStreamResponse> logs = fetchLogs(lab, start, end);

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

    private Duration normalizeRange(Duration range) {
        if (range == null || range.isNegative() || range.isZero()) {
            return DEFAULT_RANGE;
        }
        if (range.compareTo(Duration.ofDays(1)) > 0) {
            return Duration.ofDays(1);
        }
        return range;
    }

    private Map<String, String> getActualDiskDimensions(CloudWatchClient client, String instanceId) {
        Map<String, String> dimensions = new HashMap<>();
        try {
            var listMetricsResponse = client.listMetrics(builder -> builder
                    .namespace(METRIC_NAMESPACE)
                    .metricName("disk_used_percent")
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
                log.warn("CloudWatch에서 disk_used_percent 메트릭을 찾을 수 없습니다. instanceId={}", instanceId);
            }
        } catch (CloudWatchException ex) {
            log.warn("디스크 dimension 조회 실패, 기본값 사용: {}", ex.getMessage());
        }
        return dimensions;
    }

    private List<MetricDataQuery> buildQueries(String instanceId, Map<String, String> diskDimensions) {
        List<MetricDataQuery> queries = new ArrayList<>();
        for (MetricKey key : MetricKey.values()) {
            MetricDataQuery query = buildMetricDataQuery(key, instanceId, diskDimensions);
            queries.add(query);
            log.debug("메트릭 쿼리 생성 - 타입: {}, 메트릭: {}, Dimension 개수: {}", 
                    key.responseKey, key.metricName, query.metricStat().metric().dimensions().size());
        }
        return queries;
    }

    private MetricDataQuery buildMetricDataQuery(MetricKey key, String instanceId, Map<String, String> diskDimensions) {
        List<Dimension> dimensions = new ArrayList<>();
        dimensions.add(Dimension.builder()
                .name("InstanceId")
                .value(instanceId)
                .build());

        if (key == MetricKey.CPU) {
            dimensions.add(Dimension.builder()
                    .name("cpu")
                    .value("cpu-total")
                    .build());
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
                                .namespace(METRIC_NAMESPACE)
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

        log.debug("CloudWatch 메트릭 응답 - 결과 개수: {}", response.metricDataResults().size());

        for (MetricDataResult result : response.metricDataResults()) {
            MetricKey key = resolveMetricKey(result.id());
            if (key == null) {
                log.debug("지원되지 않는 MetricDataResult id: {}", result.id());
                continue;
            }
            List<LabMetricPoint> points = new ArrayList<>();
            List<Instant> timestamps = result.timestamps();
            List<Double> values = result.values();
            
            log.debug("{} 메트릭 - 데이터 포인트 개수: {}", key.responseKey, timestamps.size());
            
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

    private List<LabLogStreamResponse> fetchLogs(Lab lab, Instant start, Instant end) {
        CloudWatchLogsClient client = cloudWatchLogsClientProvider.getClient(lab.getRegion());
        List<LabLogStreamResponse> streams = new ArrayList<>();

        for (String suffix : LOG_STREAM_SUFFIXES) {
            String streamName = lab.getInstanceId() + "/" + suffix;
            try {
                GetLogEventsResponse response = client.getLogEvents(GetLogEventsRequest.builder()
                        .logGroupName(logGroupName)
                        .logStreamName(streamName)
                        .startTime(start.toEpochMilli())
                        .endTime(end.toEpochMilli())
                        .limit(LOG_EVENT_LIMIT)
                        .startFromHead(false)
                        .build());

                List<LabLogEvent> events = response.events().stream()
                        .map(this::toLogEvent)
                        .collect(Collectors.toList());

                streams.add(new LabLogStreamResponse(
                        logGroupName,
                        streamName,
                        events
                ));
            } catch (CloudWatchLogsException ex) {
                String reason = ex.awsErrorDetails() != null
                        ? ex.awsErrorDetails().errorMessage()
                        : ex.getMessage();
                log.warn("CloudWatch 로그 조회 실패 - labUuid={}, stream={}, reason={}",
                        lab.getUuid(), streamName, reason);
                streams.add(new LabLogStreamResponse(
                        logGroupName,
                        streamName,
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


