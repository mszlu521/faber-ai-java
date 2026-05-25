package com.mszlu.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mszlu.ai.common.config.JsonbTypeHandler;
import com.mszlu.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document_chunks")
public class DocumentChunk extends BaseEntity {

    @TableField("document_id")
    private UUID documentId;

    @TableField("kb_id")
    private UUID knowledgeBaseId;

    @TableField("es_id")
    private String elasticSearchId;

    @TableField("chunk_index")
    private Integer chunkIndex;

    @TableField("content")
    private String content;

    @TableField("token_count")
    private Integer tokenCount;

    @TableField(value = "meta_info", typeHandler = JsonbTypeHandler.class)
    private String metaInfo;

    @TableField("status")
    private String status;
    @Getter
    @RequiredArgsConstructor
    public enum Status {
        PENDING("pending", "待处理"),
        INDEXED("indexed", "已索引"),
        FAILED("failed", "索引失败");

        private final String value;
        private final String desc;
    }

}
