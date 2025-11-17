package com.labhub.CveLabhubBack.cve_lab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GuacamoleConnectionResponse(
        String identifier,
        @JsonProperty("connectionUrl") String connectionUrl
) {}
