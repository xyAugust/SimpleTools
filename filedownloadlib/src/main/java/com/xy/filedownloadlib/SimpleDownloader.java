package com.xy.filedownloadlib;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

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
    private Call loader;
    private Handler handler;
    private static final HashMap<String, DownloadInfo> loaderMap = new HashMap<>(1);

    public SimpleDownloader() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        handler = new Handler(callback);
    }

    private DownloadInfo downloadInfo;
    private SimpleDownloadListener listener;

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public SimpleDownloader setListener(SimpleDownloadListener listener) {
        this.listener = listener;
        return this;
    }

    public SimpleDownloader createTask(String url, String savePath, long fileLength) {
        downloadInfo = new DownloadInfo(url);
        downloadInfo.setSavePath(savePath);
        downloadInfo.setTotalLength(fileLength);
        return this;
    }

    public SimpleDownloader createTask(String url, String savePath) {
        return createTask(url, savePath, -1);
    }

    public void download(String url, String savePath, SimpleDownloadListener listener){
        createTask(url, savePath).setListener(listener).download();
    }

    public void download() {
        final String downloadUrl = downloadInfo.getUrl();
        DownloadInfo info = loaderMap.get(downloadUrl);
        if (info != null) {
            if (info.getState() == DownloadInfo.START || info.getState()
                    == DownloadInfo.LOADING)
                return;
        }
        THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                FileOutputStream fileOutputStream = null;
                try {
                    SimpleDownloader.this.downloadInfo.setState(DownloadInfo.START);      // 开始
                    post(DownloadInfo.START, SimpleDownloader.this.downloadInfo);
                    long downloadLength = 0;
                    OkHttpClient httpClient = new OkHttpClient.Builder()
                            .build();
                    long contentLength = getContentLength(downloadUrl);
                    if (SimpleDownloader.this.downloadInfo.getTotalLength() < 0) {
                        SimpleDownloader.this.downloadInfo.setTotalLength(contentLength);
                    }

                    String savePath = SimpleDownloader.this.downloadInfo.getSavePath();
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
                    loader = httpClient.newCall(request);
                    loaderMap.put(downloadUrl, downloadInfo);
                    Response response = loader.execute();

                    is = response.body().byteStream();
                    fileOutputStream = new FileOutputStream(savePath, true);
                    byte[] buffer = new byte[1024 * 2];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        if (SimpleDownloader.this.downloadInfo.getState() == DownloadInfo.CANCLE) {
                            SimpleDownloader.this.downloadInfo.setError(new RuntimeException("cancle task"));
                            loaderMap.remove(downloadUrl);
                            return;
                        }
                        fileOutputStream.write(buffer, 0, len);
                        downloadLength += len;
                        SimpleDownloader.this.downloadInfo.setState(DownloadInfo.LOADING);        // 下载中
                        SimpleDownloader.this.downloadInfo.setSofarBytes(downloadLength);
                        post(DownloadInfo.LOADING, SimpleDownloader.this.downloadInfo);
                    }
                    fileOutputStream.flush();

                    SimpleDownloader.this.downloadInfo.setState(DownloadInfo.COMPLETE);           // 完成
                    post(DownloadInfo.COMPLETE, SimpleDownloader.this.downloadInfo);
                    loaderMap.remove(downloadUrl);
                } catch (Exception e) {
                    e.printStackTrace();            //  出错
                    loaderMap.remove(downloadUrl);
                    SimpleDownloader.this.downloadInfo.setError(e);
                    post(DownloadInfo.STOP, SimpleDownloader.this.downloadInfo);
                } finally {
                    closeAll(is, fileOutputStream);
                }
            }
        });
    }

    private void post(int state, DownloadInfo downloadInfo) {
        Message message = handler.obtainMessage();
        message.what = state;
        message.obj = downloadInfo;
        handler.sendMessage(message);
    }

    Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            DownloadInfo downloadInfo = (DownloadInfo) msg.obj;
            switch (msg.what) {
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

            }
            return true;
        }
    };

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

    public void cancle() {
        if (loader != null) {
            loader.cancel();
        }
        if (downloadInfo != null) {
            downloadInfo.setState(DownloadInfo.CANCLE);
        }
    }

    public void pause() {
        if (loader != null) {
            loader.cancel();
        }
        if (downloadInfo != null) {
            downloadInfo.setState(DownloadInfo.PAUSE);
        }
    }

    @NonNull
    public String getFileName(String downloadUrl) {
        if (downloadUrl == null) {
            return "null-" + System.currentTimeMillis();
        }
        return downloadUrl.substring(downloadUrl.lastIndexOf("/"));
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
