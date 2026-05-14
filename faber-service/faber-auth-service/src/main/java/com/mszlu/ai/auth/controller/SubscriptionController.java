package com.mszlu.ai.auth.controller;

import com.mszlu.ai.auth.dto.SubscriptionResponse;
import com.mszlu.ai.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscription")
public class SubscriptionController {

    @GetMapping("/current")
    public Result<SubscriptionResponse> getCurrentSubscription() {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(UUID.randomUUID().toString());
        response.setUserId(UUID.randomUUID().toString());
        response.setPlan("free");
        response.setDuration("monthly");
        response.setPaymentMethod("wechat");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        response.setStartDate(formatter.format(java.time.LocalDateTime.now()));
        response.setEndDate(formatter.format(java.time.LocalDateTime.now().plusMonths(1)));
        response.setCreatedAt(formatter.format(java.time.LocalDateTime.now()));
        response.setUpdatedAt(formatter.format(java.time.LocalDateTime.now()));
        SubscriptionResponse.PlanConfig configs = new SubscriptionResponse.PlanConfig();
        configs.setMaxAgents(10L);
        configs.setMaxWorkflows(5L);
        configs.setMaxKnowledgeBaseSize(1024L);
        response.setConfigs(configs);
        return Result.success(response);
    }
}
