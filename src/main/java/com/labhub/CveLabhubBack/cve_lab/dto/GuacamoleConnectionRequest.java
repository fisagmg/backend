package com.labhub.CveLabhubBack.cve_lab.dto;

import java.util.HashMap;
import java.util.Map;

public record GuacamoleConnectionRequest(
        String name,
        String parentIdentifier,
        String protocol,
        Map<String, Object> parameters,
        Map<String, Object> attributes
) {
    public static GuacamoleConnectionRequest from(LabCreateResponse response) {
        Map<String, Object> params = new HashMap<>();
        params.put("hostname", response.privateIp());
        params.put("port", "22");
        params.put("username", response.sshUsername());
        if (response.privateKey() != null && !response.privateKey().isBlank()) {
            params.put("private-key", response.privateKey());
        }
        if (response.sshPassword() != null && !response.sshPassword().isBlank()) {
            params.put("password", response.sshPassword());
        }
        params.put("color-scheme", "green-black");
        params.put("font-size", "12");

        Map<String, Object> attributes = Map.of(
                "max-connections", "",
                "max-connections-per-user", "",
                "weight", "",
                "failover-only", "",
                "guacd-port", "",
                "guacd-ssl", "",
                "guacd-hostname", ""
        );

        return new GuacamoleConnectionRequest(
                response.uuid(),
                "ROOT",
                "ssh",
                params,
                attributes
        );
    }
}
