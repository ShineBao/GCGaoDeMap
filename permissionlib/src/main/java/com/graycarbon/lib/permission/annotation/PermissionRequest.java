package com.graycarbon.lib.permission.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Create by GrayCarbon on 2018.8.28
 *
 * desc : 用于注解需要请求权限的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionRequest {
    // 需要申请的权限
    String[] value();
}
