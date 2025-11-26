package com.labhub.CveLabhubBack.lab_admin.dto;

import java.time.Instant;

public record LabMetricPoint(
        Instant timestamp,
        Double value
) {
}


