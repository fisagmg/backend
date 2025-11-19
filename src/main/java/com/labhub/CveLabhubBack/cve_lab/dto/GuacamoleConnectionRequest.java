package com.labhub.CveLabhubBack.cve_lab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public record GuacamoleConnectionRequest(
        String name,
        String parentIdentifier,
        String protocol,
        Map<String, Object> parameters,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty(required = false)
        Map<String, Object> attributes
) {
    public static GuacamoleConnectionRequest from(LabCreateResponse response) {
        Map<String, Object> params = new HashMap<>();
        params.put("hostname", response.privateIp());
        params.put("port", "22");
        params.put("username", response.sshUsername());
        
        // SSH 키 설정 및 정규화
        if (response.privateKey() != null && !response.privateKey().isBlank()) {
            String privateKey = response.privateKey();
            // 줄바꿈 문자 정규화 (환경변수에서 \n으로 저장된 경우)
            privateKey = privateKey.replace("\\n", "\n");
            // 실제 줄바꿈이 포함되어 있는지 확인
            boolean hasActualNewline = privateKey.contains("\n");
            boolean hasBegin = privateKey.contains("BEGIN");
            boolean hasEnd = privateKey.contains("END");
            
            // 로그 출력 (키는 마스킹)
            String maskedKey = privateKey.length() > 50 
                ? privateKey.substring(0, 20) + "..." + privateKey.substring(privateKey.length() - 20)
                : "[MASKED]";
            System.out.println("SSH Key normalization: hasActualNewline=" + hasActualNewline + 
                             ", hasBegin=" + hasBegin + ", hasEnd=" + hasEnd + 
                             ", length=" + privateKey.length());
            
            if (!hasActualNewline) {
                System.err.println("⚠️ WARNING: SSH key may not have actual newline characters!");
            }
            if (!hasBegin || !hasEnd) {
                System.err.println("⚠️ WARNING: SSH key missing BEGIN or END markers!");
            }
            
            params.put("private-key", privateKey);
        }
        
        if (response.sshPassword() != null && !response.sshPassword().isBlank()) {
            params.put("password", response.sshPassword());
        }
        
        // font-size와 color-scheme은 SSH 파라미터가 아니므로 제거
        // attributes는 빈 Map 사용 (null 대신, @JsonInclude로 null일 때 JSON에서 제외됨)

        // attributes는 빈 Map으로 설정 (Guacamole이 NPE를 방지하기 위해 필요)
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("max-connections", "");
        attributes.put("max-connections-per-user", "");
        attributes.put("weight", "");
        attributes.put("failover-only", "");
        attributes.put("guacd-encryption", "");
        attributes.put("guacd-hostname", "");
        attributes.put("guacd-port", "");
        
        return new GuacamoleConnectionRequest(
                response.uuid(),
                "ROOT",
                "ssh",
                params,
                attributes  // attributes는 빈 Map으로 설정 (null 아님)
        );
    }
}
