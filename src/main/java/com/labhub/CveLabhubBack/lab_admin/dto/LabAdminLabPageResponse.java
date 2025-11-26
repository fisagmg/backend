package com.labhub.CveLabhubBack.lab_admin.dto;

import java.util.List;

public record LabAdminLabPageResponse(
        List<LabAdminLabSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
}


