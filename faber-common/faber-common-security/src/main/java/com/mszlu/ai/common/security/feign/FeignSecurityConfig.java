package com.mszlu.ai.common.security.feign;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignSecurityConfig {
    @Bean
    public FeignUserContextInterceptor feignUserContextInterceptor() {
        return new FeignUserContextInterceptor();
    }
}
