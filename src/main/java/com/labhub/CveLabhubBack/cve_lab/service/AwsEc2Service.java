package com.labhub.CveLabhubBack.cve_lab.service;

import com.labhub.CveLabhubBack.cve_lab.client.RunnerClient;
import com.labhub.CveLabhubBack.cve_lab.dto.RunRequest;
import com.labhub.CveLabhubBack.cve_lab.dto.RunResponse;
import com.labhub.CveLabhubBack.cve_lab.exception.AwsEc2TerminationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsEc2Service {
    
    private final RunnerClient runnerClient;
    
    /**
     * AWS EC2 인스턴스 종료 (Terraform destroy 호출)
     */
    public void terminateInstance(String uuid, String cveId, String userId) {
        try {
            RunRequest request = new RunRequest(uuid, cveId, userId);
            RunResponse response = runnerClient.destroy(request);
            
            log.info("AWS EC2 instance terminated successfully: uuid={}, instanceId={}, status={}", 
                    uuid, response.uuid(), response.status());
                    
        } catch (Exception e) {
            log.error("Failed to terminate AWS EC2 instance: uuid={}", uuid, e);
            throw new AwsEc2TerminationException(
                    "Failed to terminate AWS EC2 instance for lab session: " + uuid, e);
        }
    }
}

