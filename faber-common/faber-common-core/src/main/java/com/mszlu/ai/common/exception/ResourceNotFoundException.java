package com.mszlu.ai.common.exception;

import com.mszlu.ai.common.result.ResultCode;
import lombok.Getter;

/**
 * 资源不存在异常
 * 用于明确标识资源未找到的场景，统一返回 1200 错误码
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final Integer code;

    public ResourceNotFoundException(String message) {
        super(message);
        this.code = ResultCode.RESOURCE_NOT_FOUND.getCode();
    }

    public ResourceNotFoundException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public ResourceNotFoundException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
