package com.paratera.sgri.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class FileUtils {
    /**
     * 默认构造函数.
     */
    private FileUtils() {
    }

    /**
     * 获取文件内容, ＃号开头的为注释, 忽略.
     */
    public static String getContent(String classpath) throws IOException {
        InputStream in = FileUtils.class.getResourceAsStream(classpath);
        BufferedReader bfr = new BufferedReader(new InputStreamReader(in));
        String tmp = null;
        StringBuilder sb = new StringBuilder();
        while ((tmp = bfr.readLine()) != null) {
            if (!tmp.trim().startsWith("#")) {
                sb.append(tmp).append("\n");
            }
        }
        bfr.close();
        return sb.toString();
    }

    public static void writeFile(String fileName, String data) throws IOException {
        File csvFile = new File(fileName);
        csvFile.getParentFile().mkdirs();
        org.apache.commons.io.FileUtils.writeStringToFile(csvFile, data);
    }
}
