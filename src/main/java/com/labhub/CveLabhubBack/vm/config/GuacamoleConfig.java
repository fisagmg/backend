package com.labhub.CveLabhubBack.vm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "guacamole.server")
@Getter
@Setter
public class GuacamoleConfig {
    private String baseUrl;
    private String username;
    private String password;
}