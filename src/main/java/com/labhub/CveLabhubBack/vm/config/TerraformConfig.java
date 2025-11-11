package com.labhub.CveLabhubBack.vm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "terraform.server")
@Getter
@Setter
public class TerraformConfig {
    private String host;
    private int port;
    private String user;
    private String password;
    private String projectPath;
    private String keyPath;
}