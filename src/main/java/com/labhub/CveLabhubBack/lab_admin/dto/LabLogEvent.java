package com.labhub.CveLabhubBack.lab_admin.dto;

import java.time.Instant;

public record LabLogEvent(
        Instant timestamp,
        Instant ingestionTime,
        String message
) {
}


