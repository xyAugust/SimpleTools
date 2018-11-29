package com.xy.examples;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.xy.filedownloadlib.DownLoadObserver;
import com.xy.filedownloadlib.DownloadInfo;
import com.xy.filedownloadlib.DownloadManager;
import com.xy.permissionlib.HiPermission;
import com.xy.permissionlib.PermissionCallback;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "examplesMain";
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progressBar);
        permission();
    }

    private void permission() {
        HiPermission hiPermission = new HiPermission();
        String[] permissionList = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET};
        hiPermission.requestPermission(this, permissionList, new PermissionCallback() {
            @Override
            public void onClose() {
                Log.i(TAG, "onClose: ");
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "onFinish: ");
                // continue work
            }

            @Override
            public void onDeny(String[] permissions) {
                Log.i(TAG, "onDeny: ");
            }

        });
    }

    String downloadUrl = "http://www.voidtools.com/Everything-1.4.1.895.x64-Setup.exe";

    public void download(View view) {
        DownloadManager.getInstance().setSavePath("xyDownload").download(downloadUrl, new DownLoadObserver() {
            int prePregress;

            @Override
            public void onNext(DownloadInfo downloadInfo) {
                super.onNext(downloadInfo);
                int pro = (int) (downloadInfo.getProgress() * 100 / downloadInfo.getTotal());
                if (prePregress == pro) {
                    return;
                }
                prePregress = pro;
                progressBar.setProgress(pro);
                Log.i(TAG, "onNext: " + pro);
            }

            @Override
            public void onComplete() {
                Log.i(TAG, "onComplete: ");
            }
        });
    }


}
