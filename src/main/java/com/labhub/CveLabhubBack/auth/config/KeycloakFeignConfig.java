package com.labhub.CveLabhubBack.auth.config;

import feign.Logger;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakFeignConfig {

    // Spring MVC의 HTTP 메시지 컨버터 묶음(= JSON, Form 등 직렬화/역직렬화 규칙들)
    private final ObjectFactory<HttpMessageConverters> messageConverters;

    public KeycloakFeignConfig(ObjectFactory<HttpMessageConverters> messageConverters) {
        this.messageConverters = messageConverters;
    }

    @Bean
    public Encoder feignFormEncoder() {
        // MultiValueMap을 "grant_type=password&client_id=..." 형태로 인코딩해줌
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
