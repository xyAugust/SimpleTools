package com.xy.permissionlib;

import java.io.Serializable;

public interface PermissionCallback extends Serializable {
    /**
     * 用户不同意跳转设置页面
     */
    void onClose();

    /**
     * 权限申请通过
     */
    void onFinish();

    /**
     * 被用户拒绝的权限
     *
     * @param permissions
     */
    void onDeny(String[] permissions);

}