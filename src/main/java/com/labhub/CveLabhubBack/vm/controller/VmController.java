package com.labhub.CveLabhubBack.vm.controller;

import com.labhub.CveLabhubBack.vm.dto.CreateVmRequest;
import com.labhub.CveLabhubBack.vm.dto.CreateVmResponse;
import com.labhub.CveLabhubBack.vm.service.VmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/vm")
@RequiredArgsConstructor
public class VmController {

    private final VmService vmService;

    /**
     * VM 생성
     * POST /api/v1/vm/create
     */
    @PostMapping("/create")
    public ResponseEntity<?> createVm(@RequestBody CreateVmRequest request) {
        try {
            log.info("VM 생성 요청: vmName={}, instanceType={}",
                    request.getVmName(), request.getInstanceType());

            CreateVmResponse response = vmService.createVm(request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("VM 생성 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "VM 생성 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * VM 삭제
     * DELETE /api/v1/vm/{vmId}
     */
    @DeleteMapping("/{vmId}")
    public ResponseEntity<?> deleteVm(@PathVariable String vmId) {
        try {
            log.info("VM 삭제 요청: vmId={}", vmId);

            vmService.deleteVm(vmId);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "VM이 성공적으로 삭제되었습니다."
            ));

        } catch (IllegalArgumentException e) {
            log.error("VM 삭제 실패: VM을 찾을 수 없음", e);
            return ResponseEntity.status(404).body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("VM 삭제 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "VM 삭제 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 웹 터미널 URL 조회
     * GET /api/v1/vm/{vmId}/terminal
     */
    @GetMapping("/{vmId}/terminal")
    public ResponseEntity<?> getTerminalUrl(@PathVariable String vmId) {
        try {
            log.info("터미널 URL 조회 요청: vmId={}", vmId);

            String terminalUrl = vmService.getTerminalUrl(vmId);

            return ResponseEntity.ok(Map.of(
                    "terminalUrl", terminalUrl
            ));

        } catch (IllegalArgumentException e) {
            log.error("터미널 URL 조회 실패: VM을 찾을 수 없음", e);
            return ResponseEntity.status(404).body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("터미널 URL 조회 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "터미널 URL 조회 실패: " + e.getMessage()
            ));
        }
    }
}