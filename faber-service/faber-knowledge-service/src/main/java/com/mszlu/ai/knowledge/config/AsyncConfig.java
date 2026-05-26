package com.mszlu.ai.knowledge.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置 - 用于文档处理的异步执行
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * 文档处理专用线程池
     */
    @Bean("documentProcessingExecutor")
    public ThreadPoolTaskExecutor documentProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：根据CPU核心数设置
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        
        // 最大线程数：核心线程数的2倍
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        
        // 队列容量：允许排队等待的任务数
        executor.setQueueCapacity(100);
        
        // 线程名称前缀：便于日志追踪
        executor.setThreadNamePrefix("doc-processing-");
        
        // 拒绝策略：调用者运行策略，确保任务不会丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 空闲线程存活时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("文档处理线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
}
