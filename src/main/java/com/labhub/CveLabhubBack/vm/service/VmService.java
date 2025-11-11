package com.labhub.CveLabhubBack.vm.service;

import com.labhub.CveLabhubBack.vm.client.GuacamoleClient;
import com.labhub.CveLabhubBack.vm.dto.CreateVmRequest;
import com.labhub.CveLabhubBack.vm.dto.CreateVmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class VmService {

    private final TerraformService terraformService;
    private final GuacamoleClient guacamoleClient;

    // 메모리에 VM 정보 저장 (vmId -> connectionId, instanceId 매핑)
    private final Map<String, VmData> vmStore = new ConcurrentHashMap<>();

    /**
     * VM 생성
     */
    public CreateVmResponse createVm(CreateVmRequest request) throws Exception {
        log.info("VM 생성 시작: vmName={}", request.getVmName());

        // 1. Terraform으로 EC2 생성
        Map<String, String> ec2Info = terraformService.createEc2();
        String instanceId = ec2Info.get("instance_id");
        String publicIp = ec2Info.get("public_ip");
        String privateIp = ec2Info.get("private_ip");

        log.info("EC2 생성 완료: instanceId={}, publicIp={}", instanceId, publicIp);

        // 2. SSH Private Key 읽기
        String privateKey = terraformService.getPrivateKey();

        // 3. Guacamole 로그인 (토큰 발급)
        String guacToken = guacamoleClient.login();

        // 4. Guacamole에 SSH 연결 생성
        String connectionId = guacamoleClient.createConnection(
                guacToken,
                request.getVmName(),
                publicIp,
                privateKey
        );

        log.info("Guacamole SSH 연결 생성 완료: connectionId={}", connectionId);

        // 5. 웹 터미널 URL 생성
        String terminalUrl = guacamoleClient.getTerminalUrl(guacToken, connectionId);

        // 6. VM 정보 메모리에 저장
        String vmId = UUID.randomUUID().toString();
        VmData vmData = new VmData(instanceId, connectionId, guacToken);
        vmStore.put(vmId, vmData);

        log.info("VM 생성 완료: vmId={}, instanceId={}, connectionId={}",
                vmId, instanceId, connectionId);

        // 7. 응답 생성
        return CreateVmResponse.builder()
                .vmId(vmId)
                .instanceId(instanceId)
                .publicIp(publicIp)
                .privateIp(privateIp)
                .connectionId(connectionId)
                .terminalUrl(terminalUrl)
                .build();
    }

    /**
     * VM 삭제
     */
    public void deleteVm(String vmId) throws Exception {
        log.info("VM 삭제 시작: vmId={}", vmId);

        // 1. 메모리에서 VM 정보 조회
        VmData vmData = vmStore.get(vmId);
        if (vmData == null) {
            throw new IllegalArgumentException("VM을 찾을 수 없습니다: " + vmId);
        }

        try {
            // 2. Guacamole SSH 연결 삭제
            guacamoleClient.deleteConnection(vmData.guacToken, vmData.connectionId);
            log.info("Guacamole SSH 연결 삭제 완료: connectionId={}", vmData.connectionId);

        } catch (Exception e) {
            log.warn("Guacamole 연결 삭제 실패 (계속 진행): {}", e.getMessage());
        }

        // 3. Terraform으로 EC2 삭제
        terraformService.destroyEc2();
        log.info("EC2 삭제 완료: instanceId={}", vmData.instanceId);

        // 4. 메모리에서 VM 정보 제거
        vmStore.remove(vmId);

        log.info("VM 삭제 완료: vmId={}", vmId);
    }

    /**
     * 웹 터미널 URL 조회
     */
    public String getTerminalUrl(String vmId) {
        VmData vmData = vmStore.get(vmId);
        if (vmData == null) {
            throw new IllegalArgumentException("VM을 찾을 수 없습니다: " + vmId);
        }

        return guacamoleClient.getTerminalUrl(vmData.guacToken, vmData.connectionId);
    }

    /**
     * VM 데이터 저장용 내부 클래스
     */
    private static class VmData {
        String instanceId;
        String connectionId;
        String guacToken;

        VmData(String instanceId, String connectionId, String guacToken) {
            this.instanceId = instanceId;
            this.connectionId = connectionId;
            this.guacToken = guacToken;
        }
    }
}