package com.labhub.CveLabhubBack.cve_lab.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LabCreateRequest(
        @JsonProperty("cveId") String cveName
) {}

