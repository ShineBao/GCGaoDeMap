package com.graycarbon.lib.permission.aop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import com.graycarbon.lib.permission.annotation.PermissionRequest;
import com.graycarbon.lib.permission.annotation.PermissionsCancel;
import com.graycarbon.lib.permission.annotation.PermissionsDenied;
import com.graycarbon.lib.permission.interf.IPermissionListener;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Create by GrayCarbon on 2018.8.18
 * <p>
 * desc : 该类用于标记的注解处理类，在标记有NeedRequestPermission的方法上，处理该方法的请求。
 */
@Aspect
public class PermissionAOP {

    // 权限申请的请求码
    private final static int PERMISSION_REQUEST_CODE = 1000;
    // 权限获取状态监听
    private static IPermissionListener mPermissionListener;

    private final static String EXECUTE_POINTCUT = "execution(@com.graycarbon.lib.permission.annotation.PermissionRequest * *(..))";

    @Pointcut(EXECUTE_POINTCUT + " && @annotation(permissionRequest)")
    public void pointcutNeedRequestPermission(PermissionRequest permissionRequest) {

    }

    @Around("pointcutNeedRequestPermission(permissionRequest)")
    public void adviceNeedRequestPermission(final ProceedingJoinPoint joinPoint, PermissionRequest permissionRequest) {
        Activity context = null;
        final Object object = joinPoint.getThis();
        // 判断当前对象来源
        if (object instanceof Activity)
            context = (Activity) object;
        else if (object instanceof Fragment)
            context = ((Fragment) object).getActivity();
        else if (object instanceof android.support.v4.app.Fragment)
            context = ((android.support.v4.app.Fragment) object).getActivity();
        // 为空则不继续执行
        if (context == null) return;
        // 设置监听对象
        mPermissionListener = new IPermissionListener() {
            @Override
            public void permissionCancel(String[] permissions) {
                Class<?> cls = object.getClass();
                Method[] methods = cls.getDeclaredMethods();
                if (methods == null || methods.length == 0) return;
                for (Method method : methods) {
                    // 过滤不含自定义注解PermissionDenied的方法
                    boolean isHasAnnotation = method.isAnnotationPresent(PermissionsCancel.class);
                    if (isHasAnnotation) {
                        // 操作私有方法
                        method.setAccessible(true);
                        //获取方法类型
                        Class<?>[] types = method.getParameterTypes();
                        if (types == null || types.length != 1) return;
                        //获取方法上的注解
                        PermissionsCancel aInfo = method.getAnnotation(PermissionsCancel.class);
                        if (aInfo == null) return;
                        // 解析注解上对应的信息
                        try {
                            method.invoke(object, (Object) permissions);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void permissionDenied(String[] permissions) {
                Class<?> cls = object.getClass();
                Method[] methods = cls.getDeclaredMethods();
                if (methods == null || methods.length == 0) return;
                for (Method method : methods) {
                    // 过滤不含自定义注解PermissionDenied的方法
                    boolean isHasAnnotation = method.isAnnotationPresent(PermissionsDenied.class);
                    if (isHasAnnotation) {
                        // 操作私有方法
                        method.setAccessible(true);
                        //获取方法类型
                        Class<?>[] types = method.getParameterTypes();
                        if (types == null || types.length != 1) return;
                        //获取方法上的注解
                        PermissionsDenied aInfo = method.getAnnotation(PermissionsDenied.class);
                        if (aInfo == null) return;
                        // 解析注解上对应的信息
                        try {
                            method.invoke(object, (Object) permissions);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void permissionGranted() {
                // 执行注解方法里的代码块
                try {
                    joinPoint.proceed();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        };
        // 发起请求
        executePermissionApply(context, permissionRequest.value(),
                PERMISSION_REQUEST_CODE, mPermissionListener);
    }

    /**
     * 获取权限请求的结果
     *
     * @param joinPoint 获得切面对象
     * @throws Throwable
     */
    @After("execution(* android.app.Activity.onRequestPermissionsResult(..))")
    @SuppressLint("NewApi")
    public void onRequestPermissionsResult(JoinPoint joinPoint) throws Throwable {

        Activity context = null;
        final Object object = joinPoint.getThis();
        // 判断当前对象来源
        if (object instanceof Activity)
            context = (Activity) object;
        else if (object instanceof Fragment)
            context = ((Fragment) object).getActivity();
        else if (object instanceof android.support.v4.app.Fragment)
            context = ((android.support.v4.app.Fragment) object).getActivity();
        // 为空则不继续执行
        if (context == null) return;

        //获取本类的所有方法，存放入数组
        Object[] objects = joinPoint.getArgs();
        int requestCode = (int) objects[0];
        String[] permissions = (String[]) objects[1];
        int[] grantResults = (int[]) objects[2];

        if (requestCode == PERMISSION_REQUEST_CODE) {
            ArrayList<String> permissionsCancel = new ArrayList<>();
            ArrayList<String> permissionsDenied = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                // 1、APP没有申请这个权限的话，返回false
                // 2、用户拒绝时，勾选了不再提示的话，返回false
                // 3、用户拒绝，但是没有勾选不再提示的话，返回true
                // 因此如果想在第一次就给用户提示，需要记录权限是否申请过，没有申请过的话，强制弹窗提示，而不能根据这个方法的返回值来
                boolean grantResult = PackageManager.PERMISSION_GRANTED == grantResults[i];
                boolean shouldShow = context.shouldShowRequestPermissionRationale(permissions[i]);
                if (!grantResult) { // 权限被用户拒绝
                    if (shouldShow) {
                        // 可再次申请的权限，用户拒绝但未勾选不再提示
                        permissionsCancel.add(permissions[i]);
                    } else {
                        // 永久拒绝的权限，用户拒绝并勾选不再提示
                        permissionsDenied.add(permissions[i]);
                    }
                }
            }
            if (permissionsCancel.size() > 0) {
                mPermissionListener.permissionCancel(permissionsCancel.toArray(new String[permissionsCancel.size()]));
            }
            if (permissionsDenied.size() > 0) {
                mPermissionListener.permissionDenied(permissionsDenied.toArray(new String[permissionsDenied.size()]));
            }
            // 所有权限都通过，调用通过
            if (permissionsCancel.size() == 0 && permissionsDenied.size() == 0) {
                mPermissionListener.permissionGranted();
            }
        }
    }

    /**
     * 执行权限的申请
     *
     * @param permissions 所需要的权限
     */
    private void executePermissionApply(Activity activity, String[] permissions, int requestCode, IPermissionListener iPermissionListener) {
        String[] hasRequests = checkPermissionsIsHave(activity, permissions);
        if (hasRequests.length > 0) { // 需要进行权限申请
            ActivityCompat.requestPermissions(activity, hasRequests, requestCode);
        } else { // 都已获取权限
            mPermissionListener.permissionGranted();
        }
    }

    /**
     * 检查权限是否都已拥有，若没有获得，则返回没有获取到的权限数组
     *
     * @param permissions 被检查的权限数组
     * @return 未获得的权限数组
     */
    @SuppressLint("NewApi")
    private String[] checkPermissionsIsHave(Activity activity, String[] permissions) {
        ArrayList<String> list = new ArrayList<>();
        for (String permission : permissions) {
            if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                list.add(permission);
            }
        }
        return list.toArray(new String[list.size()]);
    }

}
