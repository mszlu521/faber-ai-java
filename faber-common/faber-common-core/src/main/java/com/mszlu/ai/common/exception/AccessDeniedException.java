package com.mszlu.ai.common.exception;

import com.mszlu.ai.common.result.ResultCode;
import lombok.Getter;

/**
 * 权限不足异常
 * 用于明确标识无权限操作的场景，统一返回 403 错误码
 */
@Getter
public class AccessDeniedException extends RuntimeException {

    private final Integer code;

    public AccessDeniedException(String message) {
        super(message);
        this.code = ResultCode.FORBIDDEN.getCode();
    }

    public AccessDeniedException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public AccessDeniedException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
