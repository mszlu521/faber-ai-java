package com.mszlu.ai.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mszlu.ai.common.security.context.UserContext;
import com.mszlu.ai.common.utils.JsonUtils;
import com.mszlu.ai.knowledge.dto.CreateKnowledgeBaseRequest;
import com.mszlu.ai.knowledge.dto.KnowledgeBaseListRequest;
import com.mszlu.ai.knowledge.dto.KnowledgeBaseListResponse;
import com.mszlu.ai.knowledge.dto.KnowledgeBaseResponse;
import com.mszlu.ai.knowledge.entity.KnowledgeBase;
import com.mszlu.ai.knowledge.mapper.KnowledgeBaseMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    public KnowledgeBaseResponse createKnowledgeBase(@Valid CreateKnowledgeBaseRequest request) {
        UUID userId = UserContext.getUserId();
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setCreatorId(userId);
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setEmbeddingModelName(request.getEmbeddingModelName());
        knowledgeBase.setEmbeddingModelProvider(request.getEmbeddingModelProvider());
        knowledgeBase.setChatModelName(request.getChatModelName());
        knowledgeBase.setChatModelProvider(request.getChatModelProvider());
        knowledgeBase.setEmbeddingDimension(request.getVectorDimension());
        knowledgeBase.setStorageType(KnowledgeBase.StorageType.MILVUS);
        knowledgeBase.setDocumentCount(0);
        knowledgeBase.setStatus(KnowledgeBase.Status.ACTIVE);
        List<String> tags = request.getTags() != null ? request.getTags() : List.of();
        knowledgeBase.setTags(JsonUtils.toJson(tags));
        knowledgeBaseMapper.insert(knowledgeBase);
        return convertToResponse(knowledgeBase);
    }

    private KnowledgeBaseResponse convertToResponse(KnowledgeBase knowledgeBase) {
        KnowledgeBaseResponse response = new KnowledgeBaseResponse();
        response.setId(knowledgeBase.getId());
        response.setName(knowledgeBase.getName());
        response.setDescription(knowledgeBase.getDescription());
        response.setEmbeddingModelName(knowledgeBase.getEmbeddingModelName());
        response.setEmbeddingModelProvider(knowledgeBase.getEmbeddingModelProvider());
        response.setChatModelName(knowledgeBase.getChatModelName());
        response.setChatModelProvider(knowledgeBase.getChatModelProvider());
        response.setEmbeddingDimension(knowledgeBase.getEmbeddingDimension());
        response.setStorageType(knowledgeBase.getStorageType().getValue());
        response.setDocumentCount(knowledgeBase.getDocumentCount());
        response.setTags(JsonUtils.fromJsonToList(knowledgeBase.getTags(), String.class));
        response.setTotalSize(0L);
        response.setCreatedAt(knowledgeBase.getCreatedAt().toInstant().toEpochMilli());
        response.setUpdatedAt(knowledgeBase.getUpdatedAt().toInstant().toEpochMilli());
        response.setStorageConfig(knowledgeBase.getStorageConfig());
        return response;
    }

    public KnowledgeBaseListResponse listKnowledgeBase(KnowledgeBaseListRequest.KnowledgeBaseQueryParam request) {
        UUID userId = UserContext.getUserId();
        Page<KnowledgeBase> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getCreatorId, userId);
        if (request.getSearch() != null && !request.getSearch().isEmpty()){
            queryWrapper.like(KnowledgeBase::getName, request.getSearch());
        }
        queryWrapper.orderByDesc(KnowledgeBase::getCreatedAt);
        Page<KnowledgeBase> knowledgeBasePage = knowledgeBaseMapper.selectPage(page, queryWrapper);
        KnowledgeBaseListResponse response = new KnowledgeBaseListResponse();
        response.setKnowledgeBases(knowledgeBasePage.getRecords().stream().map(this::convertToResponse).toList());
        response.setTotal(knowledgeBasePage.getTotal());
        return response;
    }
}
