package com.graycarbon.lib.permission.interf;

/**
 * Create by GrayCarbon on 2018.8.28
 *
 * desc : 用于监听权限的获取过程
 */
public interface IPermissionListener {

    /**
     * 权限被取消，并未勾选“不再提示”
     */
    void permissionCancel(String[] permissions);

    /**
     * 权限被拒，无法再次申请，并已勾选“不再提示”
     */
    void permissionDenied(String[] permissions);

    /**
     * 权限被允许，只有在所有的权限都被允许后，才会调用该方法
     */
    void permissionGranted();

}
