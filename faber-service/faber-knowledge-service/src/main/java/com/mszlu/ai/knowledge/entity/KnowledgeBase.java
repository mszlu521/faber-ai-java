package com.mszlu.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mszlu.ai.common.config.JsonbTypeHandler;
import com.mszlu.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_bases")
public class KnowledgeBase extends BaseEntity {

    @TableField("creator_id")
    private UUID creatorId;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("chat_model_name")
    private String chatModelName;

    @TableField("chat_model_provider")
    private String chatModelProvider;

    @TableField("embedding_model_name")
    private String embeddingModelName;

    @TableField("embedding_model_provider")
    private String embeddingModelProvider;

    @TableField("embedding_dimension")
    private Integer embeddingDimension;

    @TableField("storage_type")
    private StorageType storageType;

    @TableField(value = "storage_config", typeHandler = JsonbTypeHandler.class)
    private String storageConfig;

    @TableField("document_count")
    private Integer documentCount;

    @TableField(value = "tags", typeHandler = JsonbTypeHandler.class)
    private String tags;

    @TableField("status")
    private Status status;

    /**
     * 存储类型枚举
     */
    @Getter
    @RequiredArgsConstructor
    public enum StorageType {
        /** Elasticsearch */
        ES("es"),
        /** Milvus 向量数据库 */
        MILVUS("milvus");

        private final String value;

    }

    /**
     * 状态枚举
     */
    @Getter
    @RequiredArgsConstructor
    public enum Status {
        /** 激活/启用 */
        ACTIVE("active"),
        /** 禁用 */
        INACTIVE("inactive"),
        /** 处理中 */
        PROCESSING("processing"),
        /** 错误 */
        ERROR("error");

        private final String value;

    }
}
