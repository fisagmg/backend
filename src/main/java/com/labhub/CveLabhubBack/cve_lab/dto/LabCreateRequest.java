package com.labhub.CveLabhubBack.cve_lab.dto;

public record LabCreateRequest(
        String userId,
        String cveId
) {}