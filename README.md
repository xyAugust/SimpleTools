# SimpleTools
简单工具

> 两个单独的lib

* 一键请求权限
* 大文件断点下载

```java

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
```

```java

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

```
