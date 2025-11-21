package com.labhub.CveLabhubBack.cve_lab.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

// terraform runner에게 받는 응답
@JsonIgnoreProperties(ignoreUnknown = true)
public record RunResponse(
        String status,
        String uuid,
        @JsonProperty("cveId") String cveName,
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