SimpleTools
简单工具

> 两个单独的lib
>* 一键请求权限
>* 大文件断点下载
   

---

> 一键请求权限

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

> 大文件断点下载

```java

    public void download(View view) {
        SimpleDownloader simpleDownloader = new SimpleDownloader();
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

```
> * 提供取消和暂停的方法,在回调里面会有对应的状态.

```
        simpleDownloader.cancle();
        simpleDownloader.pause();
```

---

> 需要的配置

* 在项目root gradle
```gradle
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

* 在module gradle
```gradle
	dependencies {
	        implementation 'com.github.xyAugust:SimpleTools:v0.22'
	}
```

**注意** 可能还需要如下配置
```gradle
	android {
            ...
            compileOptions {
                sourceCompatibility JavaVersion.VERSION_1_8
                targetCompatibility JavaVersion.VERSION_1_8
            }
    }
```
