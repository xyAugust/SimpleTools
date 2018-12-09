package com.xy.filedownloadlib;

public abstract class SimpleDownloadListener {
    public void onStart(DownloadInfo task) {
    }

    public abstract void onProgress(DownloadInfo task, long sofarBytes);

    public abstract void onComplete(DownloadInfo task);

    public abstract void onStop(DownloadInfo task, Throwable error);
}
