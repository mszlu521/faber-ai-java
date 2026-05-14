package com.mszlu.ai.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.mszlu.ai")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.mszlu.ai")
public class LLMServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LLMServiceApplication.class, args);
    }
}
