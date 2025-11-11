package com.labhub.CveLabhubBack.vm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVmResponse {
    private String vmId;           // UUID
    private String instanceId;     // AWS EC2 Instance ID (i-xxx)
    private String publicIp;       // Public IP
    private String privateIp;      // Private IP
    private String connectionId;   // Guacamole Connection ID
    private String terminalUrl;    // 웹 터미널 URL
}