package com.labhub.CveLabhubBack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

<<<<<<< HEAD
@EnableFeignClients(basePackages = "com.labhub.CveLabhubBack.client")
=======
@EnableFeignClients(basePackages = "com.labhub.CveLabhubBack.service")
>>>>>>> parent of b147818 (Revert "db 연동 테스트 코드 추가")
@SpringBootApplication
public class CveLabhubBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(CveLabhubBackApplication.class, args);
	}

}
