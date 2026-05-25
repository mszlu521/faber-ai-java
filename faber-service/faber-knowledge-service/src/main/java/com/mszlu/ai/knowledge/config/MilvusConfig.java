package com.mszlu.ai.knowledge.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MilvusConfig {
    private final MilvusProperties milvusProperties;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(milvusProperties.getClient().getHost())
                .withPort(milvusProperties.getClient().getPort())
                .withDatabaseName(milvusProperties.getDatabaseName());
        String userName = milvusProperties.getClient().getUsername();
        String password = milvusProperties.getClient().getPassword();
        if (userName != null && !userName.isEmpty() && password != null && !password.isEmpty()) {
            builder.withAuthorization(userName, password);
        }
        return new MilvusServiceClient(builder.build());
    }
}
