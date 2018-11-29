package com.xy.filedownloadlib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 关闭IO流
 */

public class IOUtils {
    public static void closeAll(InputStream is, OutputStream os) {
        try {
            if (is != null){
                is.close();
            }
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
