package com.mszlu.ai.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    
    SUCCESS(200, "success"),
    NOT_FOUND(404, "404"),
    BAD_REQUEST(400, "Bad request"),
    UNAUTHENTICATED(401, "Unauthenticated"),
    FORBIDDEN(403, "Forbidden"),
    ERROR(500, "error"),
    
    // 认证相关 1000-1099
    UNAUTHORIZED(1001, "Unauthorized"),
    TOKEN_EXPIRED(1002, "Token expired"),
    TOKEN_INVALID(1003, "Token invalid"),
    USER_NOT_FOUND(1004, "User not found"),
    PASSWORD_ERROR(1005, "Password error"),
    USER_DISABLED(1006, "User disabled"),
    EMAIL_NOT_VERIFIED(1007, "Email not verified"),
    VERIFY_CODE_ERROR(1008, "email verify code error"),
    
    // 参数相关 1100-1199
    PARAM_ERROR(1100, "Parameter error"),
    PARAM_MISSING(1101, "Missing required parameter"),
    PARAM_INVALID(1102, "Invalid parameter"),
    
    // 资源相关 1200-1299
    RESOURCE_NOT_FOUND(1200, "Resource not found"),
    RESOURCE_ALREADY_EXISTS(1201, "Resource already exists"),
    RESOURCE_CONFLICT(1202, "Resource conflict"),
    
    // 业务相关 1300-1399
    BUSINESS_ERROR(1300, "Business error"),
    OPERATION_FAILED(1301, "Operation failed"),
    
    // 系统相关 1400-1499
    SYSTEM_ERROR(1400, "System error"),
    SERVICE_UNAVAILABLE(1401, "Service unavailable"),
    RATE_LIMIT_EXCEEDED(1402, "Rate limit exceeded"),

    AGENT_NOT_FOUND(15001, "Agent not found"),
    PROVIDER_NOT_DELETE(1503, "厂商下有模型，不能删除");
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
