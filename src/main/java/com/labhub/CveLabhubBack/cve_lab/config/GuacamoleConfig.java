package com.labhub.CveLabhubBack.cve_lab.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "guacamole")
public class GuacamoleConfig {
    private String baseUrl;
    private String iframeBaseUrl;
    private String adminUsername;
    private String adminPassword;
    private String dataSource = "mysql";
}