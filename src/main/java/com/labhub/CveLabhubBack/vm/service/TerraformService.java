package com.labhub.CveLabhubBack.vm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labhub.CveLabhubBack.vm.client.SshClient;
import com.labhub.CveLabhubBack.vm.config.TerraformConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerraformService {

    private final SshClient sshClient;
    private final TerraformConfig terraformConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Terraform으로 EC2 생성
     * @return Map with keys: instance_id, public_ip, private_ip
     */
    public Map<String, String> createEc2() throws Exception {
        String projectPath = terraformConfig.getProjectPath();

        // 1. terraform apply 실행
        String applyCommand = String.format(
                "cd %s && terraform apply -auto-approve",
                projectPath
        );

        log.info("Terraform Apply 실행 중...");
        String applyOutput = sshClient.executeCommand(applyCommand);
        log.info("Terraform Apply 완료:\n{}", applyOutput);

        // 2. terraform output으로 정보 추출
        String outputCommand = String.format(
                "cd %s && terraform output -json",
                projectPath
        );

        log.info("Terraform Output 조회 중...");
        String outputJson = sshClient.executeCommand(outputCommand);

        // 3. JSON 파싱
        Map<String, String> result = parseOutputJson(outputJson);
        log.info("EC2 생성 완료: {}", result);

        return result;
    }

    /**
     * Terraform으로 EC2 삭제
     */
    public void destroyEc2() throws Exception {
        String projectPath = terraformConfig.getProjectPath();

        String destroyCommand = String.format(
                "cd %s && terraform destroy -auto-approve",
                projectPath
        );

        log.info("Terraform Destroy 실행 중...");
        String destroyOutput = sshClient.executeCommand(destroyCommand);
        log.info("Terraform Destroy 완료:\n{}", destroyOutput);
    }

    /**
     * SSH Private Key 읽기
     */
    public String getPrivateKey() throws Exception {
        String keyPath = terraformConfig.getKeyPath();

        String command = String.format("cat %s", keyPath);
        String privateKey = sshClient.executeCommand(command);

        log.info("SSH Private Key 읽기 완료");
        return privateKey;
    }

    /**
     * terraform output -json 결과 파싱
     *
     * 예시 JSON:
     * {
     *   "instance_id": {"value": "i-xxx"},
     *   "public_ip": {"value": "13.125.94.37"},
     *   "private_ip": {"value": "10.0.1.86"}
     * }
     */
    private Map<String, String> parseOutputJson(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        Map<String, String> result = new HashMap<>();
        result.put("instance_id", root.get("instance_id").get("value").asText());
        result.put("public_ip", root.get("public_ip").get("value").asText());
        result.put("private_ip", root.get("private_ip").get("value").asText());

        return result;
    }
}