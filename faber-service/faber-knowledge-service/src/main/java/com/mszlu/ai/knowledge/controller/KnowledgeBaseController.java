package com.mszlu.ai.knowledge.controller;

import com.mszlu.ai.common.result.Result;
import com.mszlu.ai.knowledge.dto.*;
import com.mszlu.ai.knowledge.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

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
    @GetMapping("/{id}")
    public Result<KnowledgeBaseResponse> getKnowledgeBase(@PathVariable UUID id) {
        KnowledgeBaseResponse response = knowledgeBaseService.getKnowledgeBase(id);
        return Result.success(response);
    }
    @PutMapping("/{id}")
    public Result<KnowledgeBaseResponse> updateKnowledgeBase(@PathVariable String id, @Valid @RequestBody CreateKnowledgeBaseRequest request) {
        KnowledgeBaseResponse response = knowledgeBaseService.updateKnowledgeBase(id, request);
        return Result.success(response);
    }

    @PostMapping("/{id}/documents")
    public Result<UploadDocumentResponse> addDocuments(@PathVariable String id,
                                                       @RequestParam("file")MultipartFile[] files) {
        UploadDocumentResponse response = knowledgeBaseService.uploadDocument(id, files);
        return Result.success(response);
    }
    @GetMapping("/{id}/documents")
    public Result<DocumentListResponse> listDocuments(@PathVariable UUID id,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) String sortBy,
                                                      @RequestParam(required = false) String sortOrder,
                                                      @RequestParam(required = false) String search,
                                                      @RequestParam(required = false) String status) {
        return Result.success(knowledgeBaseService.listDocuments(id, page, pageSize, search, sortBy, sortOrder, status));
    }
    @DeleteMapping("/{id}/documents/{documentId}")
    public Result<Void> deleteDocument(@PathVariable UUID id, @PathVariable UUID documentId) {
       knowledgeBaseService.deleteDocument(id, documentId);
       return Result.success();
    }
    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledgeBase(@PathVariable UUID id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return Result.success();
    }
    @PostMapping("/{id}/search")
    public Result<SearchKnowledgeBaseResponse> search(@PathVariable UUID id, @RequestBody SearchKnowledgeBaseRequest request) {
        return Result.success(knowledgeBaseService.search(id, request));
    }
}
