package com.labhub.CveLabhubBack.config;

import feign.Logger;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign이 form-urlencoded 형식으로 데이터를 보낼 수 있도록 설정하는 Config 클래스
 */
@Configuration
public class KeycloakFeignConfig {

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
