package com.labhub.CveLabhubBack.cve_lab.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class LabConfig {
    
    @Value("${lab.max-ttl-minutes:120}")
    private int maxTtlMinutes;
    
    @Value("${lab.extend-unit-minutes:30}")
    private int extendUnitMinutes;
}

