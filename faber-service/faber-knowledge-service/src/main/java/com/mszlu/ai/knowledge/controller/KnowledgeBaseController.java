package com.mszlu.ai.knowledge.controller;

import com.mszlu.ai.common.result.Result;
import com.mszlu.ai.knowledge.dto.CreateKnowledgeBaseRequest;
import com.mszlu.ai.knowledge.dto.KnowledgeBaseListRequest;
import com.mszlu.ai.knowledge.dto.KnowledgeBaseListResponse;
import com.mszlu.ai.knowledge.dto.KnowledgeBaseResponse;
import com.mszlu.ai.knowledge.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    public Result<KnowledgeBaseResponse> createKnowledgeBase(@Valid  @RequestBody CreateKnowledgeBaseRequest request) {
        KnowledgeBaseResponse response = knowledgeBaseService.createKnowledgeBase(request);
        return Result.success(response);
    }
    @PostMapping("/list")
    public Result<KnowledgeBaseListResponse> listKnowledgeBase(@Valid @RequestBody KnowledgeBaseListRequest request) {
        KnowledgeBaseListResponse response = knowledgeBaseService.listKnowledgeBase(request.getParams());
        return Result.success(response);
    }
}
