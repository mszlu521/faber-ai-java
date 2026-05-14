package com.mszlu.ai.common.constants;

//常量
public interface CommonConstants {

    String UTF = "UTF-8";

    String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    // 请求头
    String HEADER_USER_ID = "X-User-Id";
    String HEADER_USERNAME = "X-Username";
    String HEADER_USER_ROLE = "X-User-Role";
    String HEADER_AUTHORIZATION = "Authorization";
    String HEADER_BEARER = "Bearer ";

    // 用户角色
    String ROLE_ADMIN = "admin";
    String ROLE_USER = "user";

    // 时间格式
    String DATE_FORMAT = "yyyy-MM-dd";
    String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
}
