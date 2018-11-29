package com.xy.permissionlib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.util.ArrayList;

public class HiPermission {

    public void requestPermission(Activity activity, String[] permissionList, PermissionCallback callback) {
        ArrayList<String> permissions = new ArrayList<>();
        for (String perm : permissionList) {
            if (checkPermission(activity, perm)) {
                continue;
            }
            permissions.add(perm);
        }
        if (permissions.isEmpty()) {
            if (callback != null) {
                callback.onFinish();
                return;
            }
        }

        PermissionActivity.setPermssinCallback(callback);
        Intent intent = new Intent(activity, PermissionActivity.class);
        intent.putExtra(PermissionActivity.DATA_PERMISSIONS, permissions);
        activity.startActivity(intent);
    }

    public void requestPermission(Activity activity, String permission, PermissionCallback callback) {
        requestPermission(activity, new String[]{permission}, callback);
    }

    public static boolean checkPermission(Context context, String permission) {
        int checkPermission = context.checkSelfPermission(permission);
        if (checkPermission == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }
}
