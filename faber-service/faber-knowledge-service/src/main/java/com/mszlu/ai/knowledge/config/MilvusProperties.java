package com.mszlu.ai.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.vectorstore.milvus")
public class MilvusProperties {

    private ClientConfig client = new ClientConfig();

    private String databaseName = "default";

    @Data
    public static class ClientConfig {
        private String host;
        private int port;
        private String username;
        private String password;
    }
}
