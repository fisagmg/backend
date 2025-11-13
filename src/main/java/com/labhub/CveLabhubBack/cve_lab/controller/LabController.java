package com.labhub.CveLabhubBack.cve_lab.controller;

import com.labhub.CveLabhubBack.cve_lab.dto.LabCreateRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.LabCreateResponse;
import com.labhub.CveLabhubBack.cve_lab.dto.RunRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.RunResponse;
import com.labhub.CveLabhubBack.cve_lab.service.LabService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/labs")
@RequiredArgsConstructor
public class LabController {
    private final LabService labService;

    @PostMapping("/create")
    public ResponseEntity<RunResponse> create(@RequestBody LabCreateRequest req,
                                              @AuthenticationPrincipal Jwt jwt) {
        String userId = (jwt != null && jwt.hasClaim("sub"))
                ? jwt.getClaimAsString("sub")
                : null;
        RunResponse response = labService.create(userId, req);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/destroy")
    public ResponseEntity<RunResponse> destroy(@RequestBody RunRequest req,
                                               @AuthenticationPrincipal Jwt jwt) {
        String userId = (jwt != null && jwt.hasClaim("sub"))
                ? jwt.getClaimAsString("sub")
                : null;

        RunResponse response = labService.destroy(userId, req);
        return ResponseEntity.ok(response);
    }
}
