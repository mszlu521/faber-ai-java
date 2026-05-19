package com.mszlu.ai.core.tools.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 工具参数信息定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterInfo {

    /**
     * 参数类型
     */
    private String type;

    /**
     * 数组元素的类型信息，仅当 type 为 array 时有效
     */
    private ParameterInfo elemInfo;

    /**
     * 对象类型的子参数，仅当 type 为 object 时有效
     */
    private Map<String, ParameterInfo> subParams;

    /**
     * 参数描述
     */
    private String desc;

    /**
     * 枚举值，仅当 type 为 string 时有效
     */
    private List<String> enumValues;

    /**
     * 是否必填
     */
    private boolean required;
}
