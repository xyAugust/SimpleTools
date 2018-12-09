package com.xy.filedownloadlib;

public class DownloadInfo {
    public static final int START = 0x01;
    public static final int LOADING = 0x02;
    public static final int STOP = 0x03;
    public static final int COMPLETE = 0x04;
    public static final int CANCLE = 0x21;
    public static final int PAUSE = 0x22;

    public static final long TOTAL_ERROR = -1;//获取文件大小失败
    private String url;
    private long totalLength;
    private long sofarBytes;
    private String fileName;
    private String fileMd5;
    private String savePath;
    private int state;
    private Throwable error;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public DownloadInfo(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(long totalLength) {
        this.totalLength = totalLength;
    }

    public long getSofarBytes() {
        return sofarBytes;
    }

    public void setSofarBytes(long sofarBytes) {
        this.sofarBytes = sofarBytes;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }
}