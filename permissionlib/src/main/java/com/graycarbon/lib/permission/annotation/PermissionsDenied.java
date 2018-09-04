package com.graycarbon.lib.permission.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Create by GrayCarbon on 2018.8.28
 * <p>
 * desc : 用于注解被用户拒绝的权限
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionsDenied {

}
