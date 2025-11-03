package com.labhub.CveLabhubBack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.labhub.CveLabhubBack.client")
@SpringBootApplication
public class CveLabhubBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(CveLabhubBackApplication.class, args);
	}

}
