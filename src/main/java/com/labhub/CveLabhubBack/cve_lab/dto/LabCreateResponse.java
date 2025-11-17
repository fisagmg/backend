package com.labhub.CveLabhubBack.cve_lab.dto;

/**
 * Lab 생성 응답 DTO
 * 
 * 초기 생성 시: guacamoleUrl = null
 * GuacamoleService 완료 후: guacamoleUrl = "https://guac.server/guacamole/#/client/{connectionId}"
 */
public record LabCreateResponse(
        String uuid,
        String cveId,
        String privateIp,
        String hostname,
        String instanceId,
        String status,
        String tfstatePath,
        String guacamoleUrl,      // 초기: null, 완료 후: iframe URL
        String sshUsername,       // 환경변수 또는 기본값 "ubuntu"
        String sshPassword,        // 환경변수 (선택적, null 가능)
        String privateKey         // 환경변수에서 항상 가져옴
) {
    /**
     * Guacamole URL을 추가한 새로운 LabCreateResponse를 반환합니다.
     */
    public LabCreateResponse withGuacamoleUrl(String url) {
        return new LabCreateResponse(
                uuid, cveId, privateIp, hostname, instanceId,
                status, tfstatePath, url,
                sshUsername, sshPassword, privateKey
        );
    }
}