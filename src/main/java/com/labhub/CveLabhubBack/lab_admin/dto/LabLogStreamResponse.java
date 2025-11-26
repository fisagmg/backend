package com.labhub.CveLabhubBack.lab_admin.dto;

import java.util.List;

public record LabLogStreamResponse(
        String logGroup,
        String logStream,
        List<LabLogEvent> events
) {
}


