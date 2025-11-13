package com.labhub.CveLabhubBack.cve_lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RunResponse(
        String status,
        String uuid,
        @JsonProperty("cveId") String cveId,
        @JsonProperty("tfstate_path") String tfstatePath,
        Map<String, RunnerOutput> outputs
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RunnerOutput(
            boolean sensitive,
            String type,
            String value
    ) {}
}