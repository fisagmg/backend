package com.labhub.CveLabhubBack.cve_lab.dto;

public record LabCreateResponse(
        String uuid,
        String cveId,
        String privateIp,
        String hostname
){}