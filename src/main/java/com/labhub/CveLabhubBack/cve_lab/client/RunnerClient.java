package com.labhub.CveLabhubBack.cve_lab.client;

import com.labhub.CveLabhubBack.cve_lab.dto.RunRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.RunResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class RunnerClient {

    private final RestTemplate terraformRestTemplate;
    private final String baseUrl;

    public RunnerClient(
            @Qualifier("terraformRestTemplate") RestTemplate terraformRestTemplate,
            @Value("${terraform.server.url}") String baseUrl) {
        this.terraformRestTemplate = terraformRestTemplate;
        this.baseUrl = baseUrl;
    }

    public RunResponse create(RunRequest request) {
        return post("/api/labs/create", request);
    }

    public RunResponse destroy(RunRequest request) {
        return post("/api/labs/destroy", request);
    }

    private RunResponse post(String path, RunRequest request) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(path)
                .toUriString();

        log.info("Sending to Runner: {} (timeout: 5 minutes)", request);

        try {
            ResponseEntity<RunResponse> responseEntity =
                    terraformRestTemplate.postForEntity(url, request, RunResponse.class);

            RunResponse body = responseEntity.getBody();
            if (body == null) {
                throw new RunnerClientException("Runner returned empty response for path " + path);
            }

            log.info("Runner response received: uuid={}, status={}", body.uuid(), body.status());
            return body;
        } catch (RestClientException ex) {
            log.error("Failed to call runner endpoint {}: {}", path, ex.getMessage(), ex);
            throw new RunnerClientException("Failed to call runner endpoint " + path, ex);
        }
    }
}

