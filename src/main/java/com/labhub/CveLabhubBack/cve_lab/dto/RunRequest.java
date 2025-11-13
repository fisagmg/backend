package com.labhub.CveLabhubBack.cve_lab.dto;

public record RunRequest(
        String uuid,
        String cveId,
        String userId
) {}
