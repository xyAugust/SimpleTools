package com.xy.examples;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xy.filedownloadlib.DownloadInfo;
import com.xy.filedownloadlib.SimpleDownloadListener;
import com.xy.filedownloadlib.SimpleDownloader;
import com.xy.permissionlib.HiPermission;
import com.xy.permissionlib.PermissionCallback;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "examplesMain";
    private ProgressBar progressBar;
    private SimpleDownloader simpleDownloader;

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
                // TODO: 2018-12-9 do something
            }

            @Override
            public void onDeny(String[] permissions) {
                Log.i(TAG, "onDeny: ");
            }

        });
    }

    String downloadUrl = "http://gdown.baidu.com/data/wisegame/617ea76d7dd3a0ea/jinritoutiao_701.apk";

    public void download(View view) {
        simpleDownloader = new SimpleDownloader();
        String fileName = simpleDownloader.getFileName(downloadUrl);
        String savePath = Environment.getExternalStorageDirectory().toString() + File.separator + fileName;

        simpleDownloader.download(downloadUrl, savePath, new SimpleDownloadListener() {
            int preProgress;

            @Override
            public void onProgress(DownloadInfo task, long sofarBytes) {
                int progress = (int) (sofarBytes * 100 / task.getTotalLength());
                if (preProgress == progress) {
                    return;
                }
                preProgress = progress;
                progressBar.setProgress(progress);
                ((TextView) view).setText(progress + " %");
                Log.i(TAG, "onProgress: " + preProgress);
            }

            @Override
            public void onComplete(DownloadInfo task) {
                Log.i(TAG, "onComplete: ");
            }

            @Override
            public void onStop(DownloadInfo task, Throwable error) {
                Log.i(TAG, "onStop: ");
                if (task.getState() == DownloadInfo.CANCLE) {
                    // TODO: 2018-12-9 do something
                } else if (task.getState() == DownloadInfo.PAUSE) {
                    // TODO: 2018-12-9 do something
                }
            }
        });
    }

    public void pause(View view) {
        if (simpleDownloader != null) {
            simpleDownloader.pause();
        }
    }

    public void cancel(View view) {
        if (simpleDownloader != null) {
            simpleDownloader.cancle();
        }
    }

}
