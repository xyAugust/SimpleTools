package com.xy.permissionlib;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

public class PermissionActivity extends AppCompatActivity {

    public static final String DATA_PERMISSIONS = "data_permissions";
    private static PermissionCallback mCallback;
    private int REQUEST_CODE = 0xf3;
    private int RE_REQUEST_CODE = 0xf4;
    ArrayList<String> unauthorizedList;
    private ArrayList<String> permissionList;
    int retry;

    public static void setPermssinCallback(PermissionCallback mCallback) {
        PermissionActivity.mCallback = mCallback;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermission();
    }

    private void requestPermission() {
        retry = 2;
        permissionList = (ArrayList<String>) getIntent().getSerializableExtra(DATA_PERMISSIONS);
        if (permissionList == null || permissionList.isEmpty()) {
            return;
        }
        unauthorizedList = new ArrayList<>();
        String[] permissionArray = listToArray(permissionList);
        ActivityCompat.requestPermissions(PermissionActivity.this, permissionArray, REQUEST_CODE);
    }

    @NonNull
    private String[] listToArray(ArrayList<String> list) {
        String[] array = new String[list.size()];
        list.toArray(array);
        return array;
    }

    private void reRequest() {
        String[] permissionArray = listToArray(unauthorizedList);
        ActivityCompat.requestPermissions(PermissionActivity.this, permissionArray, RE_REQUEST_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE || requestCode == RE_REQUEST_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    unauthorizedList.add(permissions[i]);
                }
            }
            if (mCallback != null) {
                mCallback.onDeny(listToArray(unauthorizedList));
            }
            if (!unauthorizedList.isEmpty()) {
                retry--;
                if (retry <= 0) {
                    showAlertDialog("权限申请", "应用运行需要这些权限", "取消", "确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            simpleSetting(PermissionActivity.this);
                            onClose();
                            finish();
                        }
                    });
                    return;
                }
                reRequest();
            } else {
                if (mCallback != null) {
                    mCallback.onFinish();
                    finish();
                }
            }
        }
    }

    private void showAlertDialog(String title, String msg, String cancelTxt, String PosTxt, DialogInterface.OnClickListener onClickListener) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setCancelable(false)
                .setNegativeButton(cancelTxt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .setPositiveButton(PosTxt, onClickListener).create();
        alertDialog.show();
    }

    private void onClose() {
        if (mCallback != null) {
            mCallback.onClose();
        }
    }

    public static void simpleSetting(Context context) {
        Intent intent = new Intent();
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // 将用户引导到系统设置页面
            if (Build.VERSION.SDK_INT >= 9) {
                intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
            } else if (Build.VERSION.SDK_INT <= 8) {
                intent.setAction(Intent.ACTION_VIEW);
                intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                intent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
            }
            context.startActivity(intent);
        } catch (Exception e) {//抛出异常就直接打开设置页面
            intent = new Intent(Settings.ACTION_SETTINGS);
            context.startActivity(intent);
        }
    }

}
