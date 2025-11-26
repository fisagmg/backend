package com.labhub.CveLabhubBack.lab_admin.dto;

import java.util.List;

public record LabMetricsResponse(
        String labUuid,
        String instanceId,
        String region,
        long rangeMinutes,
        List<LabMetricPoint> cpu,
        List<LabMetricPoint> memory,
        List<LabMetricPoint> disk,
        String diskPath,
        String diskDevice,
        String diskFstype,
        List<LabLogStreamResponse> logs
) {
}


