package com.mszlu.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mszlu.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("documents")
public class Document extends BaseEntity {

    @TableField("kb_id")
    private UUID knowledgeBaseId;

    @TableField("creator_id")
    private UUID creatorId;

    @TableField("name")
    private String name;

    @TableField("file_type")
    private String fileType;

    @TableField("size")
    private Long size;

    @TableField("token_count")
    private Integer tokenCount;

    @TableField("storage_key")
    private String storageKey;

    @TableField("file_hash")
    private String fileHash;

    @TableField("status")
    private String status;

    @TableField("error_message")
    private String errorMessage;

    @TableField(value = "meta_info", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object metaInfo;

    @TableField("enabled")
    private Boolean enabled;

    /**
     * 文档状态枚举
     */
    @Getter
    @RequiredArgsConstructor
    public enum Status {
        /**
         * 待处理
         */
        PENDING("pending"),
        /**
         * 处理中
         */
        PROCESSING("processing"),
        /**
         * 已完成
         */
        COMPLETED("completed"),
        /**
         * 处理失败
         */
        FAILED("failed"),
        /**
         * 已禁用
         */
        DISABLED("disabled");

        private final String value;
    }
}
