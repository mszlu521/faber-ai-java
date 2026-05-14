package com.mszlu.ai.auth.dto;

import lombok.Data;

@Data
public class SubscriptionResponse {
    private String id;
    private String userId;
    private String plan;
    private String duration;
    private String paymentMethod;
    private String startDate;
    private String endDate;
    private String createdAt;
    private String updatedAt;
    private PlanConfig configs;

    @Data
    public static class PlanConfig {
        private Long maxAgents;
        private Long maxWorkflows;
        private Long maxKnowledgeBaseSize;
    }
}
