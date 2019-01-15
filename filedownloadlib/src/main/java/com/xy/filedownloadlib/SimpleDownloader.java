package com.xy.filedownloadlib;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SimpleDownloader {
    private static final String TAG = "SimpleDownloader";
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // We want at least 2 threads and at most 4 threads in the core pool,
    // preferring to have 1 less than the CPU count to avoid saturating
    // the CPU with background work
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;
    public static final Executor THREAD_POOL_EXECUTOR;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "SimpleDownloader :" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(64);

    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }

    private OkHttpClient httpClient;
    private Handler handler;
    private static final HashMap<String, DownloadInfo> loaderMap = new HashMap<>(1);

    public SimpleDownloader() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();
        handler = new Handler(Looper.getMainLooper());
    }

    private DownloadInfo downloadInfo;
    private SimpleDownloadListener listener;

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public DownloadInfo getDownloadInfo(String url) {
        return loaderMap.get(url);
    }

    private SimpleDownloader createTask(String url, String savePath, long fileLength) {
        downloadInfo = new DownloadInfo(url);
        downloadInfo.setSavePath(savePath);
        downloadInfo.setFileName(url.substring(url.lastIndexOf("/") + 1));
        downloadInfo.setTotalLength(fileLength);
        return this;
    }

    public SimpleDownloader setListener(SimpleDownloadListener listener) {
        this.listener = listener;
        return this;
    }

    private boolean isBackground; // 是否后台下载

    public void setBackground(boolean isBackground) {
        this.isBackground = isBackground;
    }

    public void download(String url, String savePath, SimpleDownloadListener listener) {
        download(url, savePath, -1, listener);
    }

    public void download(String url, String savePath, long fileLength, SimpleDownloadListener listener) {
        DownloadInfo info = loaderMap.get(url);
        if (info != null) {
            Log.i(TAG, "download: " + info.getState());
            if (info.getState() == DownloadInfo.START || info.getState()
                    == DownloadInfo.LOADING)
                return;
        }
        createTask(url, savePath, fileLength).setListener(listener).download();
    }

    public void download() {
        THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                FileOutputStream fileOutputStream = null;
                String downloadUrl = downloadInfo.getUrl();
                try {
                    downloadInfo.setState(DownloadInfo.START);      // 开始
                    post(DownloadInfo.START, downloadInfo);
                    long downloadLength = 0;
                    long contentLength = getContentLength(downloadUrl);
                    if (downloadInfo.getTotalLength() < 0) {
                        downloadInfo.setTotalLength(contentLength);
                    }

                    String savePath = downloadInfo.getSavePath();
                    File loacalFile = new File(savePath);
                    if (loacalFile.exists()) {
                        downloadLength = loacalFile.length();
                    } else {
                        forceMkdir(new File(savePath).getParentFile());
                    }
                    Request request = new Request.Builder()
                            .addHeader("Accept-Encoding", "identity")
                            .addHeader("RANGE", "bytes=" + downloadLength + "-" + contentLength) // 续传用 已下载-总长度
                            .url(downloadUrl).build();
                    Call loader = httpClient.newCall(request);
                    loaderMap.put(downloadUrl, downloadInfo);
                    downloadInfo.setLoader(loader);
                    Response response = loader.execute();

                    is = response.body().byteStream();
                    fileOutputStream = new FileOutputStream(savePath, true);
                    byte[] buffer = new byte[1024 * 2];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        if (downloadInfo.getState() == DownloadInfo.CANCLE) {
                            RuntimeException cancle_task = new RuntimeException("stop task");
                            downloadInfo.setError(cancle_task);
                            loaderMap.remove(downloadUrl);
                            throw new RuntimeException(cancle_task);
                        }
                        fileOutputStream.write(buffer, 0, len);
                        downloadLength += len;
                        downloadInfo.setState(DownloadInfo.LOADING);        // 下载中
                        downloadInfo.setSofarBytes(downloadLength);
                        post(DownloadInfo.LOADING, downloadInfo);
                    }
                    fileOutputStream.flush();

                    downloadInfo.setState(DownloadInfo.COMPLETE);           // 完成
                    post(DownloadInfo.COMPLETE, downloadInfo);
                    loaderMap.remove(downloadUrl);
                } catch (Exception e) {
                    e.printStackTrace();            //  出错
                    if (downloadInfo.getState() == DownloadInfo.CANCLE) {
                        new File(downloadInfo.getSavePath()).delete();
                    }
                    loaderMap.remove(downloadUrl);
                    downloadInfo.setError(e);
                    post(DownloadInfo.STOP, downloadInfo);
                } finally {
                    closeAll(is, fileOutputStream);
                }
            }
        });
    }

    private void post(int state, DownloadInfo downloadInfo) {
        if (isBackground) {
            callback(state, downloadInfo);
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                callback(state, downloadInfo);
            }
        });
    }

    private void callback(int state, DownloadInfo downloadInfo) {
        if (listener == null) {
            return;
        }
        switch (state) {
            case DownloadInfo.START:
                listener.onStart(downloadInfo);
                break;
            case DownloadInfo.LOADING:
                listener.onProgress(downloadInfo, downloadInfo.getSofarBytes());
                break;
            case DownloadInfo.COMPLETE:
                listener.onComplete(downloadInfo);
                break;
            case DownloadInfo.STOP:
                listener.onStop(downloadInfo, downloadInfo.getError());
                break;
            default:
                break;
        }
    }

    public void closeAll(InputStream is, OutputStream os) {
        try {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long getContentLength(String downloadUrl) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        try {
            Response response = httpClient.newCall(request).execute();
            if (response != null && response.isSuccessful()) {
                long contentLength = response.body().contentLength();
                response.close();
                return contentLength == 0 ? DownloadInfo.TOTAL_ERROR : contentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DownloadInfo.TOTAL_ERROR;
    }

    public void cancle(String url) {
        DownloadInfo downloadInfo = loaderMap.get(url);
        if (downloadInfo != null) {
            downloadInfo.setState(DownloadInfo.CANCLE);
            Call loader = downloadInfo.getLoader();
            if (loader != null) {
                loader.cancel();
            }
        }
    }

    public void pause(String url) {
        DownloadInfo downloadInfo = loaderMap.get(url);
        if (downloadInfo != null) {
            downloadInfo.setState(DownloadInfo.PAUSE);
            Call loader = downloadInfo.getLoader();
            if (loader != null) {
                loader.cancel();
            }
        }
    }

    @NonNull
    public String getFileName(String downloadUrl) {
        if (downloadUrl == null) {
            return "null-" + System.currentTimeMillis();
        }
        return downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1);
    }

    private void forceMkdir(File directory) throws IOException {
        if (directory.exists()) {
            if (directory.isFile()) {
                String message =
                        "File "
                                + directory
                                + " exists and is "
                                + "not a directory. Unable to create directory.";
                throw new IOException(message);
            }
        } else {
            if (!directory.mkdirs()) {
                String message =
                        "Unable to create directory " + directory;
                throw new IOException(message);
            }
        }
    }

}
