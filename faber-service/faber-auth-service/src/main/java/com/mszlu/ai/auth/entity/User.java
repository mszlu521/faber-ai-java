package com.mszlu.ai.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 用户实体类 
 * 对应数据库表: users
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("users")
public class User {

    /**
     * 用户ID - 使用数据库自动生成
     */
    @TableId(type = IdType.AUTO)
    private UUID id;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 状态: 1-禁用, 2-启用, 3-待验证
     */
    @TableField("status")
    private Integer status = 3;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    private OffsetDateTime lastLoginTime;

    /**
     * 当前套餐: free-免费, basic-基础, pro-专业, enterprise-企业
     */
    @TableField("current_plan")
    private String currentPlan = "free";

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 邮箱是否已验证
     */
    @TableField("email_verified")
    private Boolean emailVerified = false;

    // ===== 业务常量定义 =====

    /** 状态: 禁用 */
    public static final Integer STATUS_DISABLED = 1;
    /** 状态: 启用 */
    public static final Integer STATUS_ENABLED = 2;
    /** 状态: 待验证 */
    public static final Integer STATUS_PENDING = 3;

    /** 套餐: 免费版 */
    public static final String PLAN_FREE = "free";
    /** 套餐: 基础版 */
    public static final String PLAN_BASIC = "basic";
    /** 套餐: 专业版 */
    public static final String PLAN_PRO = "pro";
    /** 套餐: 企业版 */
    public static final String PLAN_ENTERPRISE = "enterprise";
}