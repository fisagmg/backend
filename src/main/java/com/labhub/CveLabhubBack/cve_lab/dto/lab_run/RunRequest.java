package com.labhub.CveLabhubBack.cve_lab.dto.lab_run;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RunRequest(
        String uuid,
        @JsonProperty("cveId") String cveName,
        String userId
) {}

