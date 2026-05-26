package com.mszlu.ai.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mszlu.ai.common.exception.BusinessException;
import com.mszlu.ai.common.result.Result;
import com.mszlu.ai.common.result.ResultCode;
import com.mszlu.ai.common.security.context.UserContext;
import com.mszlu.ai.common.utils.FileUtils;
import com.mszlu.ai.common.utils.JsonUtils;
import com.mszlu.ai.core.transformer.MDDocumentTransformer;
import com.mszlu.ai.knowledge.dto.*;
import com.mszlu.ai.knowledge.entity.Document;
import com.mszlu.ai.knowledge.entity.DocumentChunk;
import com.mszlu.ai.knowledge.entity.KnowledgeBase;
import com.mszlu.ai.knowledge.feign.LlmServiceClient;
import com.mszlu.ai.knowledge.mapper.DocumentChunkMapper;
import com.mszlu.ai.knowledge.mapper.DocumentMapper;
import com.mszlu.ai.knowledge.mapper.KnowledgeBaseMapper;
import io.milvus.client.MilvusServiceClient;
import io.swagger.v3.oas.annotations.info.License;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.parser.microsoft.onenote.fsshttpb.IFSSHTTPBSerializable;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseService {
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final VectorService vectorService;
    private final ThreadPoolTaskExecutor documentProcessingExecutor;
    private final EmbeddingModelFactory embeddingModelFactory;
    private final LlmServiceClient llmServiceClient;
    private final MilvusServiceClient milvusServiceClient;
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

    public KnowledgeBaseResponse getKnowledgeBase(UUID id) {
        UUID userId = UserContext.getUserId();
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getId, id);
        queryWrapper.eq(KnowledgeBase::getCreatorId, userId);
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(queryWrapper);
        if (knowledgeBase == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        KnowledgeBaseResponse knowledgeBaseResponse = convertToResponse(knowledgeBase);
        //统计文档数和字节数
        long totalSize = documentMapper.selectList(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getKnowledgeBaseId, id)
                        .eq(Document::getCreatorId, userId))
                .stream().mapToLong(Document::getSize).sum();
        long docCount = documentMapper.selectCount(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getKnowledgeBaseId, id)
                        .eq(Document::getCreatorId, userId));
        knowledgeBaseResponse.setTotalSize(totalSize);
        knowledgeBaseResponse.setDocumentCount((int) docCount);

        vectorService.loadCollection(milvusServiceClient, id);
        return knowledgeBaseResponse;
    }

    public KnowledgeBaseResponse updateKnowledgeBase(String id, @Valid CreateKnowledgeBaseRequest request) {
        UUID userId = UserContext.getUserId();
        UUID kbId = UUID.fromString(id);
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getId, kbId)
                        .eq(KnowledgeBase::getCreatorId, userId));
        if (knowledgeBase == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        //更新的时候 我们需要做一点特殊处理
        //因为向量维度在创建向量数据库milvus就已经固定了，所以不能修改
        //所以我们这里加一个判断 如果没有文档可以修改，有文档就不能修改
        //所以向量维护只有在删除知识库下全部文档的前提下，可以修改
        if (request.getVectorDimension() != null &&
            !request.getVectorDimension().equals(knowledgeBase.getEmbeddingDimension())){
            if (documentMapper.selectCount(
                    new LambdaQueryWrapper<Document>()
                            .eq(Document::getKnowledgeBaseId, kbId)
                            .eq(Document::getCreatorId, userId)) > 0) {
                throw new BusinessException(ResultCode.VECTOR_DIMENSION_CANNOT_BE_MODIFIED);
            }
        }
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setEmbeddingModelName(request.getEmbeddingModelName());
        knowledgeBase.setEmbeddingModelProvider(request.getEmbeddingModelProvider());
        knowledgeBase.setChatModelName(request.getChatModelName());
        knowledgeBase.setChatModelProvider(request.getChatModelProvider());
        knowledgeBase.setEmbeddingDimension(request.getVectorDimension());
        List<String> tags = request.getTags() != null ? request.getTags() : List.of();
        knowledgeBase.setTags(JsonUtils.toJson(tags));
        knowledgeBaseMapper.updateById(knowledgeBase);
        return convertToResponse(knowledgeBase);
    }

    @Transactional
    public UploadDocumentResponse uploadDocument(String kbId, MultipartFile[] files) {
        UUID userId = UserContext.getUserId();
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getId, UUID.fromString(kbId));
        queryWrapper.eq(KnowledgeBase::getCreatorId, userId);
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(queryWrapper);
        if (knowledgeBase == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        UploadDocumentResponse response = new UploadDocumentResponse();
        List<UploadDocumentResponse.DocumentInfo> documentInfos = new ArrayList<>();
        int uploaded = 0;
        int failed = 0;
        for (MultipartFile file : files) {
            try {
                Document document = new Document();
                document.setKnowledgeBaseId(UUID.fromString(kbId));
                document.setCreatorId(userId);
                document.setName(file.getOriginalFilename());
                document.setFileType(FileUtils.getFileExtension(file.getOriginalFilename()));
                document.setSize(file.getSize());
                document.setFileHash(FileUtils.calculateFileHash(file.getBytes()));
                document.setTokenCount(0);
                document.setStatus(Document.Status.PENDING.getValue());
                document.setEnabled(true);
                document.setStorageKey(file.getOriginalFilename());
                documentMapper.insert(document);

                UploadDocumentResponse.DocumentInfo documentInfo = new UploadDocumentResponse.DocumentInfo();
                documentInfo.setId(document.getId());
                documentInfo.setName(document.getName());
                documentInfo.setStatus(document.getStatus());
                documentInfos.add(documentInfo);
                uploaded++;

                //上传文档，我们需要计算向量，存入向量数据库，所以这里我们异步进行处理
                final Document savedDoc = document;
                final byte[] fileBytes = file.getBytes();
                final String originalFilename = file.getOriginalFilename();
                final UUID currentUserId = userId;
                documentProcessingExecutor.execute(()->{
                            processDocumentAsync(
                                    savedDoc,
                                    fileBytes,
                                    originalFilename,
                                    currentUserId,
                                    knowledgeBase
                            );
                        }
                        );
            } catch (Exception e) {
                failed++;
                log.error("Failed to upload document", e);
            }

        }
        response.setDocuments(documentInfos);
        response.setUploaded(uploaded);
        response.setFailed(failed);
        return response;
    }

    private void processDocumentAsync(Document savedDoc,
                                          byte[] fileBytes,
                                          String originalFilename,
                                          UUID userId,
                                          KnowledgeBase kb) {
        //在异步线程中设置用户上下文，确保feign调用时能传递userId
        UserContext.set(UserContext.builder()
                .userId(userId != null ? userId.toString() : null)
                .build());
        String docId = savedDoc.getId().toString();
        String kbId = kb.getId().toString();
        log.info("process document async, docId:{}, kbId:{}", docId, kbId);
        try {
            savedDoc.setStatus(Document.Status.PROCESSING.getValue());
            documentMapper.updateById(savedDoc);
            //etl 流程
            //1. extract 提取文档内容
            ByteArrayResource resource = new ByteArrayResource(fileBytes){
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };

            List<org.springframework.ai.document.Document> extractedDocs = vectorService.extractDocument(resource);
            if (extractedDocs.isEmpty()){
                log.error("extractDocument error, docId:{}, kbId:{}", docId, kbId);
                throw new BusinessException(ResultCode.DOCUMENT_EXTRACT_ERROR);
            }
            //transform 文档分块处理
            DocumentTransformer transformer = new MDDocumentTransformer();
            List<org.springframework.ai.document.Document> chunkDocs = vectorService.transformDocuments(extractedDocs, transformer);
            if (chunkDocs.isEmpty()){
                log.error("transformDocuments error, docId:{}, kbId:{}", docId, kbId);
                throw new BusinessException(ResultCode.DOCUMENT_TRANSFORM_ERROR);
            }
            //为每个分块添加元数据
            for (int i = 0; i < chunkDocs.size(); i++){
                org.springframework.ai.document.Document chunk = chunkDocs.get(i);
                chunk.getMetadata().put("chunk_index", i);
                chunk.getMetadata().put("parent_id", docId);
                chunk.getMetadata().put("doc_id", docId);
                chunk.getMetadata().put("kb_id", kbId);
                chunk.getMetadata().put("total_chunks", chunkDocs.size());
                chunk.getMetadata().put("file_name", savedDoc.getName());
                chunk.getMetadata().put("file_type", savedDoc.getFileType());
            }
            //load
            Result<LlmServiceClient.ProviderConfigResponse> providerConfig = llmServiceClient.getProviderConfig(
                    kb.getEmbeddingModelProvider(),
                    kb.getEmbeddingModelName()
            );
            EmbeddingModel embeddingModel = embeddingModelFactory.createEmbeddingModel(
                    providerConfig.getData().getProvider(),
                    kb.getEmbeddingModelName(),
                    providerConfig.getData().getApiKey(),
                    providerConfig.getData().getApiBase(),
                    kb.getEmbeddingDimension() * 2
            );
            vectorService.vectorStore(milvusServiceClient, embeddingModel, kb);
            vectorService.loadDocuments(chunkDocs, milvusServiceClient, embeddingModel, kb);

            //保存分块到pg数据库 持久化存储
            saveDocumentChunks(savedDoc, kb, chunkDocs);
            //计算token总数
            int totalTokens = chunkDocs.stream().mapToInt(
                    doc -> doc.getText() != null ? doc.getText().length()/4 : 0)
                    .sum();
            savedDoc.setTokenCount(totalTokens);
            savedDoc.setStatus(Document.Status.COMPLETED.getValue());
            documentMapper.updateById(savedDoc);
            //更新知识库文档计数
            Long compeletedDocCount = documentMapper.selectCount(
                    new LambdaQueryWrapper<Document>()
                            .eq(Document::getKnowledgeBaseId, kb.getId())
                            .eq(Document::getStatus, Document.Status.COMPLETED.getValue())
            );
            KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(kb.getId());
            if (knowledgeBase != null){
                knowledgeBase.setDocumentCount(compeletedDocCount.intValue());
                knowledgeBaseMapper.updateById(knowledgeBase);
            }
        } catch (Exception e) {
           log.error("Failed to update document count", e);
           String errMsg = e.getMessage();
           savedDoc.setErrorMessage(errMsg);
           savedDoc.setStatus(Document.Status.FAILED.getValue());
           documentMapper.updateById(savedDoc);
        }
    }

    private void saveDocumentChunks(Document doc,
                                    KnowledgeBase kb,
                                    List<org.springframework.ai.document.Document> chunks) {
        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++){
            org.springframework.ai.document.Document chunk = chunks.get(i);
            DocumentChunk entity = new DocumentChunk();
            entity.setDocumentId(doc.getId());
            entity.setKnowledgeBaseId(kb.getId());
            entity.setChunkIndex(i);
            entity.setContent(chunk.getText());
            if (chunk.getText() != null) {
                entity.setTokenCount(chunk.getText().length()/4);
            }
            Map<String, Object> metadata = chunk.getMetadata();
            entity.setMetaInfo(JsonUtils.toJson(metadata));
            entity.setStatus(DocumentChunk.Status.ACTIVE.getValue());
            chunkEntities.add(entity);
        }
        for (DocumentChunk chunkEntity : chunkEntities) {

            documentChunkMapper.insert(chunkEntity);
        }
    }

    public DocumentListResponse listDocuments(UUID kbId,
                                              int page,
                                              int pageSize,
                                              String search,
                                              String sortBy,
                                              String sortOrder,
                                              String status) {
        UUID userId = UserContext.getUserId();
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getCreatorId, userId);
        queryWrapper.eq(KnowledgeBase::getId, kbId);
        KnowledgeBase kb = knowledgeBaseMapper.selectOne(queryWrapper);
        if (kb == null){
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        Page<Document> docPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getKnowledgeBaseId, kbId);
        wrapper.eq(Document::getCreatorId, userId);
        if (StringUtils.isNoneEmpty(search)){
            wrapper.like(Document::getName, search);
        }
        if (StringUtils.isNoneEmpty(status)){
            wrapper.eq(Document::getStatus, status);
        }
        if ("desc".equalsIgnoreCase(sortOrder)){
            wrapper.orderByDesc(Document::getCreatedAt);
        }else{
            wrapper.orderByAsc(Document::getCreatedAt);
        }
        Page<Document> documentPage = documentMapper.selectPage(docPage, wrapper);
        DocumentListResponse response = new DocumentListResponse();
        response.setTotal(documentPage.getTotal());
        response.setItems(documentPage.getRecords()
                .stream()
                .map(this::toResponse)
                .toList()
        );
        return response;
    }

    private DocumentListResponse.DocumentResponse toResponse(Document doc) {
        DocumentListResponse.DocumentResponse response = new DocumentListResponse.DocumentResponse();
        response.setId(doc.getId());
        response.setKnowledgeBaseId(doc.getKnowledgeBaseId());
        response.setName(doc.getName());
        response.setFileType(doc.getFileType());
        response.setSize(doc.getSize());
        response.setTokenCount(doc.getTokenCount());
        response.setStorageKey(doc.getStorageKey());
        response.setFileHash(doc.getFileHash());
        response.setStatus(doc.getStatus());
        response.setErrorMessage(doc.getErrorMessage());
        response.setMetaInfo(doc.getMetaInfo());
        response.setEnabled(doc.getEnabled());
        response.setCreatedAt(doc.getCreatedAt() != null ?
                doc.getCreatedAt().toInstant().toEpochMilli() : null);
        response.setUpdatedAt(doc.getUpdatedAt() != null ?
                doc.getUpdatedAt().toInstant().toEpochMilli() : null);
        return response;
    }

    @Transactional
    public void deleteDocument(UUID kbId, UUID documentId) {
        UUID userId = UserContext.getUserId();
        try {
            KnowledgeBase kb = knowledgeBaseMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getId, kbId)
                            .eq(KnowledgeBase::getCreatorId, userId)
            );
            if (kb == null) {
                throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
            }
            Document doc = documentMapper.selectOne(
                    new LambdaQueryWrapper<Document>().eq(Document::getId, documentId)
                            .eq(Document::getKnowledgeBaseId, kbId)
                            .eq(Document::getCreatorId, userId)
            );
            if (doc == null) {
                throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
            }
            int delete = documentChunkMapper.delete(
                    new LambdaQueryWrapper<DocumentChunk>()
                            .eq(DocumentChunk::getDocumentId, doc.getId())
            );
            //删除milvus
            vectorService.deleteDocumentVectors(milvusServiceClient, kb.getId(),doc.getId());
            documentMapper.deleteById(documentId);

            //如果是最后一个文档被删除，我们将整个milvus collection也删除掉
            long remainingDocCount = documentMapper.selectCount(
                    new LambdaQueryWrapper<Document>()
                    .eq(Document::getKnowledgeBaseId, kb.getId()));
            if (remainingDocCount == 0) {
                vectorService.deleteCollection(milvusServiceClient, kb.getId());
            }
        }catch (Exception e){
            log.error("Failed to delete document", e);
            throw new BusinessException(ResultCode.DELETE_DOCUMENT_FAILED);
        }
    }

    public void deleteKnowledgeBase(UUID id) {
        //判断知识库下面有文档不能删除
        long size = documentMapper.selectCount(
                new LambdaQueryWrapper<Document>().eq(Document::getKnowledgeBaseId, id));
        if (size > 0) {
            throw new BusinessException(ResultCode.KNOWLEDGE_BASE_NOT_EMPTY);
        }
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getId, id);
        UUID userId = UserContext.getUserId();
        queryWrapper.eq(KnowledgeBase::getCreatorId, userId);
        knowledgeBaseMapper.delete(queryWrapper);
    }

    public SearchKnowledgeBaseResponse search(UUID kbId,
                                              SearchKnowledgeBaseRequest request) {
        UUID userId = UserContext.getUserId();
        long start = System.currentTimeMillis();
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || !kb.getCreatorId().equals(userId)) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (request.getQuery()== null || request.getQuery().isEmpty()){
            throw new BusinessException(ResultCode.QUERY_PARAM_NOT_FOUND);
        }
        //对查询参数进行向量化
        Result<LlmServiceClient.ProviderConfigResponse> providerConfig = llmServiceClient.getProviderConfig(kb.getEmbeddingModelProvider(),
                kb.getEmbeddingModelName());
        EmbeddingModel embeddingModel = embeddingModelFactory.createEmbeddingModel(
                providerConfig.getData().getProvider(),
                kb.getEmbeddingModelName(),
                providerConfig.getData().getApiKey(),
                providerConfig.getData().getApiBase(),
                kb.getEmbeddingDimension() * 2);
        VectorStore vectorStore = vectorService.vectorStore(
                milvusServiceClient,
                embeddingModel,
                kb);
        List<org.springframework.ai.document.Document> searchResults = vectorStore.similaritySearch(SearchRequest.builder()
                .query(request.getQuery())
                .topK(10)
                .build());
        //构建响应
        SearchKnowledgeBaseResponse response = new SearchKnowledgeBaseResponse();
        response.setQuery(request.getQuery());
        response.setKbId(kb.getId());
        response.setTotal((long) searchResults.size());
        response.setTook(System.currentTimeMillis() - start);

        List<SearchKnowledgeBaseResponse.SearchResult> results = new ArrayList<>();
        for (int i = 0; i < searchResults.size(); i++){
            org.springframework.ai.document.Document doc = searchResults.get(i);
            Map<String, Object> metadata = doc.getMetadata();
            SearchKnowledgeBaseResponse.SearchResult result = new SearchKnowledgeBaseResponse.SearchResult();
            Object docId = metadata.getOrDefault("doc_id", "");
            UUID documentId = UUID.fromString(String.valueOf(docId));
            result.setDocumentId(documentId);
            result.setContent(doc.getText());
            result.setScore(doc.getScore());
            result.setMetadata( metadata);
            result.setPosition(i + 1);
            //查询文档信息
            Document document = documentMapper.selectById(documentId);
            if (document != null) {
                SearchKnowledgeBaseResponse.SearchResult.DocumentResponse documentResponse = new SearchKnowledgeBaseResponse.SearchResult.DocumentResponse();
                documentResponse.setId(document.getId());
                documentResponse.setName(document.getName());
                documentResponse.setFileType(document.getFileType());
                documentResponse.setSize(document.getSize());
                documentResponse.setStorageKey(document.getStorageKey());
                documentResponse.setFileHash(document.getFileHash());
                documentResponse.setStatus(document.getStatus());
                documentResponse.setErrorMessage(document.getErrorMessage());
                documentResponse.setMetaInfo(document.getMetaInfo());
                documentResponse.setEnabled(document.getEnabled());
                documentResponse.setCreatedAt(document.getCreatedAt().toInstant().toEpochMilli());
                documentResponse.setUpdatedAt(document.getUpdatedAt().toInstant().toEpochMilli());
                documentResponse.setKnowledgeBaseId(document.getKnowledgeBaseId());
                result.setDocument(documentResponse);
            }
            results.add(result);
        }
        response.setResults(results);
        return response;
    }
}
