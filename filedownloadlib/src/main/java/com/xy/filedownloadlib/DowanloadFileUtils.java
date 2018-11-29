package com.xy.filedownloadlib;

import java.io.File;
import java.io.IOException;

/**
 *
 */

public class DowanloadFileUtils {
    public static boolean createDir(File dir) {
        if (dir == null) {
            return false;
        }
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return false;
    }

    public static boolean createFile(File file) {
        try {
            if (file == null) {
                return false;
            }
            if (file.exists()) {
                return true;
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
