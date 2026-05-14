package com.mszlu.ai.common.exception;

import com.mszlu.ai.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    public BusinessException(ResultCode code) {
        super(code.getMessage());
        this.code = code.getCode();
    }
    public BusinessException(String message) {
        this(500, message);
    }
}