package com.mszlu.ai.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.mszlu.ai.common.config.UUIDTypeHandler;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 基础实体类，包含通用字段：ID、创建时间、更新时间、软删除时间
 * 所有业务实体应继承此类
 */
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * 主键ID，使用UUID
     */
    @TableId(type = IdType.AUTO)
    private UUID id;

    /**
     * 创建时间，自动填充，不可更新
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /**
     * 更新时间，自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    /**
     * 软删除时间，为null表示未删除
     */
    private OffsetDateTime deletedAt;

    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 标记为已删除（软删除）
     */
    public void markAsDeleted() {
        this.deletedAt = OffsetDateTime.now();
    }

    /**
     * 恢复删除
     */
    public void restore() {
        this.deletedAt = null;
    }
}
