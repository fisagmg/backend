package com.labhub.CveLabhubBack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@ConfigurationPropertiesScan(basePackages = "com.labhub.CveLabhubBack")
@EnableAsync
@EnableFeignClients(basePackages = "com.labhub.CveLabhubBack.service")
@SpringBootApplication

public class CveLabhubBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(CveLabhubBackApplication.class, args);
	}

}
