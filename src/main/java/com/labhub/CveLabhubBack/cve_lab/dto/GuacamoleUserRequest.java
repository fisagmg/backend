package com.labhub.CveLabhubBack.cve_lab.dto;

import java.util.HashMap;
import java.util.Map;

public record GuacamoleUserRequest(
        String username,
        String password,
        Map<String, String> attributes
) {

    public static GuacamoleUserRequest from(String username) {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("disabled", "");
        attrs.put("expired", "");
        attrs.put("access-window-start", "");
        attrs.put("access-window-end", "");
        attrs.put("valid-from", "");
        attrs.put("valid-until", "");
        attrs.put("timezone", "");
        return new GuacamoleUserRequest(username, "DISABLED_BY_SSO", attrs);
    }
}

