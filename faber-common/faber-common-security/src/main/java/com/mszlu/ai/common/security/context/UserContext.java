package com.mszlu.ai.common.security.context;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserContext {

    private String userId;
    private String username;
    private String role;

    public static final ThreadLocal<UserContext> CURRENT_USER = new ThreadLocal<>();

    public static void set(UserContext userContext){
        CURRENT_USER.set(userContext);
    }
    public static UserContext get(){
        return CURRENT_USER.get();
    }
    public static void clear(){
        CURRENT_USER.remove();
    }
    public static UUID getUserId(){
        UserContext userContext = get();
        return userContext == null ? null : UUID.fromString(userContext.userId);
    }
    public static String getUsername(){
        UserContext userContext = get();
        return userContext == null ? null : userContext.username;
    }
    public static String getRole(){
        UserContext userContext = get();
        return userContext == null ? null : userContext.role;
    }
}
