package com.mszlu.ai.tool.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mszlu.ai.common.config.JsonbTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@TableName("tools")
public class Tool {

    @TableId(type = IdType.AUTO)
    private UUID id;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;

    @TableField("creator_id")
    private UUID creatorId;

    private String name;
    private String description;
    private String toolType;
    private Boolean isEnable = true;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String parametersSchema;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String mcpConfig;

    public Boolean getIsEnabled() {
        return this.isEnable;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnable = isEnabled;
    }
}